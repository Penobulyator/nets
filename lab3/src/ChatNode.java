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

    private String myName;

    Connection parent = null;
    Set<Connection> neighbourSet = Collections.synchronizedSet(new TreeSet<>());

    BlockingQueue<Byte> idPool = new LinkedBlockingQueue<>(256);

    private int lossPercent;

    public ChatNode(DatagramSocket socket, int lossPercent, String myName, Connection parent) {
        this.socket = socket;
        this.lossPercent = lossPercent;
        this.myName = myName;
        this.parent = parent;
        this.messageSender = new MessageSender(socket, this);
        this.terminalMessagesListener = new TerminalMessagesListener(this);
    }



    public ChatNode(DatagramSocket socket, int lossPercent, String myName) {
        this.socket = socket;
        this.lossPercent = lossPercent;
        this.myName = myName;
        this.messageSender = new MessageSender(socket, this);
        this.terminalMessagesListener = new TerminalMessagesListener(this);
    }

    public void sendMessage(String textMessage) throws IOException {
        for (Connection neighbour : neighbourSet){
            Message message = new Message(Protocol.Flags.NO_FLAGS, UUID.randomUUID(), myName, textMessage.getBytes());
            //System.out.println("Sending message, UUID = " + message.getId().toString());
            messageSender.sendMessage(message, neighbour);
        }
    }

    public void removeNeighbour(Connection neighbour){
        neighbourSet.remove(neighbour);
        System.out.println("Neighbour " + neighbour.toString() +" doesn't respond");
    }

    private void shakeHandsWithParent() throws InterruptedException, IOException {
        UUID id = UUID.randomUUID();
        Message message = new Message(Protocol.Flags.SYN, id);
        messageSender.sendMessage(message, parent);
        neighbourSet.add(parent);
    }

    private void handleTextMessage(Connection source, Message receivedMessage) throws IOException {
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

    private void handlePacket(DatagramPacket packet) throws IOException {
        Message receivedMessage = Message.read(packet.getData(), packet.getLength());
        Connection neighbour = new Connection(packet.getAddress(), packet.getPort());

        neighbourSet.add(neighbour);

        if(receivedMessage.isServiceMessage()){
            if (receivedMessage.getFlags() == Protocol.Flags.SYN){

                //the neighbor shakes our hand
                //add connection to neighboursSet and send ack
                neighbourSet.add(neighbour);

                Message ack = new Message(Protocol.Flags.ACK, receivedMessage.getId());
                messageSender.sendAck(ack, neighbour);

                //System.out.println("Got new neighbour: " + neighbour.toString());
            }
            else if (receivedMessage.getFlags() == Protocol.Flags.ACK){

                //one of the neighbours replied to our message
                //notify messageSender about ack
                messageSender.gotAck(receivedMessage.getId());

                //System.out.println("Got ack from " + neighbour.toString() + ", UUID = " + receivedMessage.getId().toString());
            }
        }
        else {
            //we've got a text message
            //print it and send to all other neighbours
            if (new Random().nextInt(100)  > lossPercent){
                System.out.println(receivedMessage.getName() + ": " + new String(receivedMessage.getData()));
                handleTextMessage(neighbour, receivedMessage);
            }

        }
    }

    @Override
    public void run() {
        if(parent != null){
            try {
                shakeHandsWithParent();
            } catch (InterruptedException | IOException e) {
                return;
            }
        }

        Thread messageSenderThread = new Thread(messageSender);
        Thread terminalMessagesListenerThread  = new Thread(terminalMessagesListener);
        messageSenderThread.start();
        terminalMessagesListenerThread.start();

        while(!Thread.currentThread().isInterrupted()){
            try {
                byte []buf = new byte[MAX_LENGTH];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                handlePacket(packet);
            } catch (IOException e) {
                messageSenderThread.interrupt();
                terminalMessagesListenerThread.interrupt();
                return;
            }
        }
    }
}
