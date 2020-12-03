package model.server;

import model.MsgSender.MessageSender;
import model.MsgSender.MessageSenderListener;
import model.client.SnakeClient;
import model.netConfig.NetConfig;
import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class SnakeServer implements Runnable, MessageSenderListener {
    private DatagramSocket socket;

    private MessageSender messageSender;

    private SnakeModel model;

    private SnakeProto.GameConfig config;

    private Set<SnakeProto.GamePlayer> players = Collections.synchronizedSet( new HashSet<>());

    private int idCounter = 0;

    private SnakeClient client;

    public SnakeServer(DatagramSocket socket, SnakeProto.GameConfig config) {
        this.socket = socket;
        this.config = config;
        messageSender = new MessageSender(socket, this);
        model = new SnakeModel(config);
    }

    private long getMsgSeq(){
        return System.nanoTime(); //TODO: is there a better way?
    }

    private void sendState() throws IOException {
        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setState(SnakeProto.GameMessage.StateMsg.newBuilder().setState(model.getGameState()))
                .setMsgSeq(getMsgSeq())
                .build();
        for (SnakeProto.GamePlayer player: players){
            messageSender.sendMessage(gameMessage, new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
        }
    }

    private void sendAnnouncementMsgLoop(){
        while (!Thread.currentThread().isInterrupted()){

            //create announcement message
            SnakeProto.GameMessage.AnnouncementMsg announcementMsg = SnakeProto.GameMessage.AnnouncementMsg.newBuilder()
                    .setPlayers(SnakeProto.GamePlayers.newBuilder().addAllPlayers(players).build())
                    .setConfig(config)
                    .build();
            //send message
            try {
                byte[] announcementMessageBytes = announcementMsg.toByteArray();
                InetAddress announcementMsgAddress = InetAddress.getByName(NetConfig.ANNOUNCEMENT_MSG_ADDRESS);
                DatagramPacket packet = new DatagramPacket(announcementMessageBytes, announcementMessageBytes.length, announcementMsgAddress, NetConfig.ANNOUNCEMENT_MSG_PORT);
                socket.send(packet);

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            //sleep
            try {
                Thread.sleep(NetConfig.ANNOUNCEMENT_MSG_RESEND_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void stateControlLoop(){
        while (!Thread.currentThread().isInterrupted()){
            model.changeState();
            try {
                sendState();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                Thread.sleep(config.getStateDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private int getId(){
        return idCounter++;
    }

    void handlePingMsg(SnakeProto.GameMessage pingMsg, InetSocketAddress sender) throws IOException {
        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(pingMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);

    }

    void handleSteerMsg(SnakeProto.GameMessage steerMsg, InetSocketAddress sender) throws IOException {
        //change snake direction
        for (SnakeProto.GamePlayer player: players){
            if (player.getIpAddress().equals(sender.getAddress().getHostAddress()) && player.getPort() == sender.getPort()){
                model.changeDirection(player, steerMsg.getSteer().getDirection());
                break;
            }
        }

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(steerMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    void handleAckMsg (SnakeProto.GameMessage ackMsg){
        messageSender.gotAck(ackMsg.getMsgSeq());
    }

    void handleJoinMsg(SnakeProto.GameMessage joinMsg, InetSocketAddress sender) throws IOException {
        System.out.println("Got join");

        //create new player
        SnakeProto.GamePlayer player = SnakeProto.GamePlayer.newBuilder()
                .setName(joinMsg.getJoin().getName())
                .setId(getId())
                .setIpAddress(sender.getAddress().getHostAddress())
                .setPort(sender.getPort())
                .setRole(SnakeProto.NodeRole.NORMAL)
                .setType(SnakeProto.PlayerType.HUMAN)
                .setScore(0)
                .build();
        model.addPlayer(player);
        players.add(player);

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(joinMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .setReceiverId(player.getId())
                .build();
        messageSender.sendAck(ack, sender);
    }

    @Override
    public void run() {
        Thread stateControlThread = new Thread(this::stateControlLoop);
        stateControlThread.start();

        Thread announcementMsgSenderThread = new Thread(this::sendAnnouncementMsgLoop);
        announcementMsgSenderThread.start();

        Thread messageSenderThread = new Thread(messageSender);
        messageSenderThread.start();

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
                    case STEER:
                        handleSteerMsg(gameMessage, sender);
                        break;
                    case ACK:
                        handleAckMsg(gameMessage);
                        break;
                    case JOIN:
                        handleJoinMsg(gameMessage, sender);
                        break;
                    default:
                        System.out.println("Got unknown message:\n" + gameMessage.toString());
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();

                stateControlThread.interrupt();
                announcementMsgSenderThread.interrupt();
                messageSenderThread.interrupt();

                return;
            }
        }
    }

    @Override
    public void connectionNotResponding(InetSocketAddress inetSocketAddress) {
        System.out.println("Client " + inetSocketAddress.toString() + " not responding");
    }
}
