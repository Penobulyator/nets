package model.snakeNode;

import model.MsgSender.MessageSender;
import model.MsgSender.MessageSenderListener;
import model.netConfig.NetConfig;
import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SnakeNode implements Runnable, MessageSenderListener {
    //fields required for all roles
    DatagramSocket socket;

    SnakeProto.NodeRole myRole;

    private MessageSender messageSender;

    private SnakeNodeListener snakeNodeListener;

    private InetSocketAddress serverAddress;

    private String name;

    private int myId = -1;

    private SnakeProto.GameState lastReceivedState = null;

    //fields required for MASTER role
    private SnakeModel model;

    private SnakeProto.GameConfig config;

    private Set<SnakeProto.GamePlayer> players = Collections.synchronizedSet( new HashSet<>());

    private int idCounter = 0;

    private InetSocketAddress deputyAddress = null;

    //NORMAL node constructor
    public SnakeNode(DatagramSocket socket, InetSocketAddress serverAddress, SnakeNodeListener snakeClientListener, String name) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        this.snakeNodeListener = snakeClientListener;
        this.name = name;

        messageSender = new MessageSender(socket, this);

        myRole = SnakeProto.NodeRole.NORMAL;
    }

    //MASTER node constructor
    public SnakeNode(DatagramSocket socket, SnakeProto.GameConfig config, SnakeNodeListener snakeNodeListener, String name) {
        this.socket = socket;
        this.config = config;
        this.snakeNodeListener = snakeNodeListener;
        this.name = name;
        messageSender = new MessageSender(socket, this);
        model = new SnakeModel(config);
        myRole = SnakeProto.NodeRole.MASTER;
    }

    /*Functions for all roles*/
    private long getMsgSeq(){
        return System.nanoTime(); //TODO: is there a better way?
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

    private void handleRoleChangeMsg(SnakeProto.GameMessage roleChangeMsg, InetSocketAddress sender) throws IOException {

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(roleChangeMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    /*Functions for NORMAL role*/

    void handleStateMsg(SnakeProto.GameMessage stateMsg, InetSocketAddress sender) throws IOException {
        SnakeProto.GameState gameState = stateMsg.getState().getState();
        lastReceivedState = gameState;
        boolean dead = true;

        //check if we are alive
        for (SnakeProto.GamePlayer player: gameState.getPlayers().getPlayersList()){
            //find myself in players list
            if (player.getId() == myId){

                //we are alive
                snakeNodeListener.updateState(stateMsg.getState().getState());
                dead = false;
                break;
            }
        }

        if (dead)
            snakeNodeListener.gameOver();

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(stateMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
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

    void handleErrorMsg(SnakeProto.GameMessage errorMsg, InetSocketAddress sender) throws IOException {
        //TODO: do something with error

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(errorMsg.getMsgSeq())
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

        if (myRole != SnakeProto.NodeRole.MASTER)
            messageSender.sendMessage(gameMessage, serverAddress);
        else{
            for (SnakeProto.GamePlayer player: players){
                if (player.getId() == myId){
                    model.changeDirection(player, direction);
                    break;
                }
            }
        }
    }

    /*Functions for MASTER role*/

    private void becomeMaster(){
        if (lastReceivedState == null){
            model = new SnakeModel(config);
        }
        else {
            model = new SnakeModel(lastReceivedState);
        }

        Thread stateControlThread = new Thread(this::stateControlLoop);
        stateControlThread.start();

        Thread announcementMsgSenderThread = new Thread(this::sendAnnouncementMsgLoop);
        announcementMsgSenderThread.start();
    }

    private int getId(){
        return idCounter++;
    }

    private void sendState() throws IOException {
        SnakeProto.GameState state = model.getGameState();
        snakeNodeListener.updateState(state);
        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setState(SnakeProto.GameMessage.StateMsg.newBuilder().setState(state))
                .setMsgSeq(getMsgSeq())
                .build();
        for (SnakeProto.GamePlayer player: players){
            if (player.getId() != myId){
                messageSender.sendMessage(gameMessage, new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
            }
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

    private void handleSteerMsg(SnakeProto.GameMessage steerMsg, InetSocketAddress sender) throws IOException {
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



    private void handleJoinMsg(SnakeProto.GameMessage joinMsg, InetSocketAddress sender) throws IOException {
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
        Thread messageSenderThread = new Thread(messageSender);
        messageSenderThread.start();

        //send join to server if we are a NORMAL node
        if (myRole == SnakeProto.NodeRole.NORMAL){
            try {
                sendJoin();
            } catch (IOException e) {
                snakeNodeListener.gotException(e);
                return;
            }
        }
        else if (myRole == SnakeProto.NodeRole.MASTER){
            becomeMaster();
            try {
                SnakeProto.GamePlayer myPlayer = SnakeProto.GamePlayer.newBuilder()
                        .setName(name)
                        .setId(getId())
                        .setIpAddress(InetAddress.getLocalHost().getHostAddress())
                        .setPort(socket.getPort())
                        .setRole(SnakeProto.NodeRole.MASTER)
                        .setScore(0)
                        .build();
                myId = myPlayer.getId();
                model.addPlayer(myPlayer);
                players.add(myPlayer);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        while (!Thread.currentThread().isInterrupted()){
            try {
                //receive packet
                DatagramPacket packet = new DatagramPacket(new byte[NetConfig.MAX_MSG_LENGTH], NetConfig.MAX_MSG_LENGTH);
                socket.receive(packet);

                //read game message from bytes
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                //System.out.println(gameMessage.toString());

                //get sender
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
                        if (myRole != SnakeProto.NodeRole.MASTER)
                            handleErrorMsg(gameMessage, sender);
                        break;
                    case ROLE_CHANGE:
                        if (myRole != SnakeProto.NodeRole.MASTER)
                            handleRoleChangeMsg(gameMessage, sender);
                        break;
                    case STEER:
                        if (myRole == SnakeProto.NodeRole.MASTER)
                            handleSteerMsg(gameMessage, sender);
                        break;
                    case JOIN:
                        if (myRole == SnakeProto.NodeRole.MASTER)
                            handleJoinMsg(gameMessage, sender);
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

    @Override
    public void connectionNotResponding(InetSocketAddress address) {

    }
}
