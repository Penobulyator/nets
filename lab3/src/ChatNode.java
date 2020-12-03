import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ChatNode implements Runnable{
    private static final int MAX_LENGTH = 256;

    private DatagramSocket socket;

    private MessageSender messageSender;
    private TerminalMessagesListener terminalMessagesListener;
    private PingSender pingSender;

    private String myName;

    //<neighbour, its deputy>
    Map<Connection, Connection> deputyMap = new TreeMap<>();
    private Connection myDeputy = null;

    Connection parent = null;
    Set<Connection> neighbourSet = Collections.synchronizedSet(new TreeSet<>());

    private int lossPercent;

    public ChatNode(DatagramSocket socket, int lossPercent, String myName, Connection parent) {
        this(socket, lossPercent, myName);

        this.parent = parent;
        this.myDeputy = parent;
    }

    public ChatNode(DatagramSocket socket, int lossPercent, String myName) {
        this.socket = socket;
        this.lossPercent = lossPercent;
        this.myName = myName;

        this.messageSender = new MessageSender(socket, this);
        this.terminalMessagesListener = new TerminalMessagesListener(this);
        this.pingSender = new PingSender(messageSender, neighbourSet);
    }

    public void sendMessage(String textMessage) throws IOException {
        for (Connection neighbour : neighbourSet){
            Message message = new Message(Protocol.Flags.NO_FLAGS, UUID.randomUUID(), myName, textMessage.getBytes());
            messageSender.sendMessage(message, neighbour);
        }
    }

    public void removeNeighbour(Connection neighbour) throws IOException {
        //tell message sender to stop waiting for reply from this neighbour
        messageSender.stopWaiting(neighbour);

        //remove neighbour from neighbourSet
        neighbourSet.remove(neighbour);
        System.out.println("Neighbour " + neighbour.toString() +" doesn't respond");

        //connect to the neighbours deputy
        Connection newNeighbour = deputyMap.get(neighbour);
        if (newNeighbour != null){
            System.out.println("Reconnecting to " + newNeighbour.toString());
            shakeHands(newNeighbour);
            deputyMap.remove(neighbour);
        }

        //pick a new deputy if this neighbour was our deputy
        if (neighbour.equals(myDeputy)){
            if (!neighbourSet.isEmpty()){
                changeMyDeputy(neighbourSet.iterator().next());
            }else {
                myDeputy = null;
            }
        }
    }

    private void shakeHands(Connection neighbour) throws IOException {
        UUID id = UUID.randomUUID();
        Message message = new Message(Protocol.Flags.SYN, id);
        messageSender.sendMessage(message, neighbour);
        neighbourSet.add(neighbour);
    }

    private void sendDeputy(Connection neighbour) throws IOException {
        if (!neighbour.equals(myDeputy)){
            String deputyString = myDeputy.toString();
            Message deputyNotification = new Message(Protocol.Flags.SET_DEPUTY, UUID.randomUUID(),
                    "", deputyString.getBytes());
            messageSender.sendMessage(deputyNotification, neighbour);
        }
    }

    private void changeMyDeputy(Connection myAlternate) throws IOException {
        this.myDeputy = myAlternate;
        for(Connection neighbour: neighbourSet){
            sendDeputy(neighbour);
        }
    }

    private void handleTextMessage(Connection source, Message receivedMessage) throws IOException {
        System.out.println(receivedMessage.getName() + ": " + new String(receivedMessage.getData()));
        for (Connection neighbour : neighbourSet){
            if (neighbour.equals(source)){

                //send ack
                Message ack = new Message(Protocol.Flags.ACK, receivedMessage.getId());
                messageSender.sendAck(ack, neighbour);
            }
            else {
                Message newMessage = new Message(Protocol.Flags.NO_FLAGS,UUID.randomUUID(), receivedMessage.getName(), receivedMessage.getData());
                messageSender.sendMessage(newMessage, neighbour);
            }
        }
    }

    private void sendAck(Message message, Connection neighbour) throws IOException {
        Message ack = new Message((byte) (Protocol.Flags.ACK), message.getId());
        messageSender.sendAck(ack, neighbour);
    }

    private void handlePacket(DatagramPacket packet) throws IOException {
        Message receivedMessage = Message.read(packet.getData(), packet.getLength());
        Connection neighbour = new Connection(packet.getAddress(), packet.getPort());

        neighbourSet.add(neighbour);

        if(receivedMessage.isServiceMessage()){
            if (receivedMessage.getFlags() == Protocol.Flags.SYN){

                //the neighbor shakes our hand
                //add connection to neighboursSet and send ack
                //send him our deputy
                neighbourSet.add(neighbour);

                sendAck(receivedMessage, neighbour);

                changeMyDeputy(neighbour);

                if (myDeputy == null){
                    changeMyDeputy(neighbour);
                }
                else {
                    sendDeputy(neighbour);
                }
            }
            else if (receivedMessage.getFlags() == Protocol.Flags.ACK){

                //one of the neighbours replied to our message
                //notify messageSender about ack
                messageSender.gotAck(receivedMessage.getId());

                //System.out.println("Got ack from " + neighbour.toString() + ", UUID = " + receivedMessage.getId().toString());
            }
            else if(receivedMessage.getFlags() == Protocol.Flags.PING){
                sendAck(receivedMessage, neighbour);
            }
            else if (receivedMessage.getFlags() == Protocol.Flags.SET_DEPUTY){
                //neighbour lets us now about its alternate

                //add alternate to map
                String deputyString = new String(receivedMessage.getData()).substring(0, receivedMessage.getData().length);
                deputyMap.put(neighbour, Connection.fromString(deputyString));

                //send ack
                sendAck(receivedMessage, neighbour);
            }
        }
        else {
            //we've got a text message
            if (new Random().nextInt(100)  > lossPercent){
                handleTextMessage(neighbour, receivedMessage);
            }

        }
    }

    @Override
    public void run() {
        if(parent != null){
            try {
                shakeHands(parent);
                changeMyDeputy(parent);
            } catch (IOException e) {
                return;
            }
        }

        Thread messageSenderThread = new Thread(messageSender);
        Thread terminalMessagesListenerThread  = new Thread(terminalMessagesListener);
        Thread pingSenderThread = new Thread(pingSender);

        messageSenderThread.start();
        terminalMessagesListenerThread.start();
        pingSenderThread.start();

        while(!Thread.currentThread().isInterrupted()){
            try {
                byte []buf = new byte[MAX_LENGTH];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                handlePacket(packet);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                messageSenderThread.interrupt();
                terminalMessagesListenerThread.interrupt();
                pingSenderThread.interrupt();
                return;
            }
        }
    }
}
