package model.client;

import model.MsgSender.MessageSender;
import model.MsgSender.MessageSenderListener;
import model.netConfig.NetConfig;
import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class SnakeClient implements Runnable, MessageSenderListener {
    private DatagramSocket socket;

    private MessageSender messageSender;

    private SnakeClientListener snakeClientListener;

    private InetSocketAddress serverAddress;

    private String name;

    private int myId = -1;

    public SnakeClient(DatagramSocket socket, InetSocketAddress serverAddress, SnakeClientListener snakeClientListener, String name) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.snakeClientListener = snakeClientListener;
        this.name = name;

        messageSender = new MessageSender(socket, this);
    }

    private long getMsgSeq(){
        return System.nanoTime(); //TODO: is there a better way?
    }

    void sendJoin() throws IOException {
        SnakeProto.GameMessage.JoinMsg joinMsg = SnakeProto.GameMessage.JoinMsg.newBuilder()
                .setName(name)
                .build();

        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(getMsgSeq())
                .build();

        messageSender.sendMessage(gameMessage, serverAddress);
    }

    @Override
    public void connectionNotResponding(InetSocketAddress inetSocketAddress) {
        System.out.println("Player " + inetSocketAddress.toString() + " not responding");

        //TODO: do something
    }

    void handlePingMsg(SnakeProto.GameMessage pingMsg, InetSocketAddress sender) throws IOException {
        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(pingMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);

    }

    void handleAckMsg (SnakeProto.GameMessage ackMsg){
        if (myId == -1)
            myId = ackMsg.getReceiverId();

        messageSender.gotAck(ackMsg.getMsgSeq());
    }

    void handleStateMsg(SnakeProto.GameMessage stateMsg, InetSocketAddress sender) throws IOException {
        snakeClientListener.updateState(stateMsg.getState().getState());

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(stateMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    void handleErrorMsg(SnakeProto.GameMessage errorMsg, InetSocketAddress sender) throws IOException {
        //TODO: do something with error

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(errorMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    private void handleRoleChangeMsg(SnakeProto.GameMessage roleChangeMsg, InetSocketAddress sender) throws IOException {

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(roleChangeMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    public void changeDirection(SnakeProto.Direction direction) throws IOException {
        SnakeProto.GameMessage.SteerMsg steerMsg = SnakeProto.GameMessage.SteerMsg.newBuilder()
                .setDirection(direction)
                .build();

        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setMsgSeq(getMsgSeq())
                .build();

        messageSender.sendMessage(gameMessage, serverAddress);
    }

    @Override
    public void run() {

        Thread messageSenderThread = new Thread(messageSender);
        messageSenderThread.start();

        try {
            sendJoin();
        } catch (IOException e) {
            snakeClientListener.gotException(e);
            return;
        }


        while (!Thread.currentThread().isInterrupted()){
            try {
                //receive packet
                DatagramPacket packet = new DatagramPacket(new byte[NetConfig.MAX_MSG_LENGTH], NetConfig.MAX_MSG_LENGTH);
                socket.receive(packet);

                //read game message from bytes
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                //System.out.println(gameMessage.toString());

                //create connection object
                InetSocketAddress sender = new InetSocketAddress(packet.getAddress(), packet.getPort());

                //handle message
                switch (gameMessage.getTypeCase()){
                    case PING:
                        handlePingMsg(gameMessage, sender);
                        break;
                    case ACK:
                        handleAckMsg(gameMessage);
                        break;
                    case STATE:
                        handleStateMsg(gameMessage, sender);
                        break;
                    case ERROR:
                        handleErrorMsg(gameMessage, sender);
                        break;
                    case ROLE_CHANGE:
                        handleRoleChangeMsg(gameMessage, sender);
                        break;
                    default:
                        System.out.println("Got unknown message:\n" + gameMessage.toString());
                        break;

                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

        }
    }
}
