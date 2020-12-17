package model.snakeNode;

import model.net.MsgSender.MessageSender;
import model.net.MsgSender.MessageSenderListener;
import model.net.NetConfig;
import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    private boolean iAmInGame = false;

    //fields required for MASTER role
    private SnakeModel model;

    private SnakeProto.GameConfig config;

    private Map<Integer, SnakeProto.GamePlayer> players = new ConcurrentHashMap<>();

    private InetSocketAddress deputyAddress = null;

    //NORMAL node constructor
    public SnakeNode(DatagramSocket socket, SnakeProto.GameConfig config, SnakeNodeListener snakeClientListener, String name, InetSocketAddress serverAddress) {
        this.socket = socket;
        this.config = config;
        this.name = name;
        this.snakeNodeListener = snakeClientListener;
        this.serverAddress = serverAddress;

        messageSender = new MessageSender(socket, this, config.getNodeTimeoutMs());

        myRole = SnakeProto.NodeRole.NORMAL;
    }

    //MASTER node constructor
    public SnakeNode(DatagramSocket socket, SnakeProto.GameConfig config, SnakeNodeListener snakeNodeListener, String name) {
        this.socket = socket;
        this.config = config;
        this.name = name;
        this.snakeNodeListener = snakeNodeListener;
        messageSender = new MessageSender(socket, this, config.getNodeTimeoutMs());
        model = new SnakeModel(config);
        myRole = SnakeProto.NodeRole.MASTER;
        iAmInGame = true;
    }

    /*Functions for all roles*/
    private long getMsgSeq(){
        return System.nanoTime(); //TODO: is there a better way?
    }

    private void sendPingLoop(){
        while (!Thread.currentThread().isInterrupted()){
                try {
                    if (myRole != SnakeProto.NodeRole.MASTER){
                        SnakeProto.GameMessage ping = SnakeProto.GameMessage.newBuilder()
                                .setMsgSeq(getMsgSeq())
                                .setPing(SnakeProto.GameMessage.PingMsg.getDefaultInstance())
                                .build();
                        messageSender.sendMessage(ping, serverAddress);
                    }
                    else {
                        for (SnakeProto.GamePlayer player: players.values()){
                            if (player.getId() != myId){
                                SnakeProto.GameMessage ping = SnakeProto.GameMessage.newBuilder()
                                        .setMsgSeq(getMsgSeq())
                                        .setPing(SnakeProto.GameMessage.PingMsg.getDefaultInstance())
                                        .build();
                                messageSender.sendAck(ping, new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
                            }
                        }
                    }
                } catch (IOException e) {
                    return;
                }

            try {
                Thread.sleep(config.getPingDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
        if (ackMsg.hasReceiverId()){
            myId = ackMsg.getReceiverId();
            iAmInGame = true;
        }

        messageSender.gotAck(ackMsg.getMsgSeq());
    }

    private void handleRoleChangeMsg(SnakeProto.GameMessage roleChangeMsg, InetSocketAddress sender) throws IOException {
        SnakeProto.GameMessage.RoleChangeMsg roleChange = roleChangeMsg.getRoleChange();

        //change role
        if (roleChange.getSenderRole() == SnakeProto.NodeRole.MASTER && roleChange.getReceiverRole() == SnakeProto.NodeRole.DEPUTY){
            myRole = roleChange.getReceiverRole();
            serverAddress = sender;
        } else if (roleChange.getSenderRole() == SnakeProto.NodeRole.DEPUTY && roleChange.getReceiverRole() == SnakeProto.NodeRole.NORMAL) {
            deputyAddress = sender;
        }

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(roleChangeMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    /*Functions for NORMAL role*/
    void handleStateMsg(SnakeProto.GameMessage stateMsg, InetSocketAddress sender) throws IOException {
        if (!iAmInGame)
            return;

        SnakeProto.GameState gameState = stateMsg.getState().getState();
        lastReceivedState = gameState;
        boolean iAmDead = true;

        //check if we are alive
        for (SnakeProto.GamePlayer player: gameState.getPlayers().getPlayersList()){
            if (player.getId() != myId && myRole == SnakeProto.NodeRole.DEPUTY && player.getRole() != SnakeProto.NodeRole.MASTER){
                SnakeProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakeProto.GameMessage.RoleChangeMsg.newBuilder()
                        .setSenderRole(SnakeProto.NodeRole.DEPUTY)
                        .setReceiverRole(SnakeProto.NodeRole.NORMAL)
                        .build();
                SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                        .setMsgSeq(getMsgSeq())
                        .setRoleChange(roleChangeMsg)
                        .setReceiverId(player.getId())
                        .setSenderId(myId)
                        .build();
                messageSender.sendMessage(gameMessage, new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
            }
            //find myself in players list
            if (player.getId() == myId){

                //we are alive
                snakeNodeListener.updateState(stateMsg.getState().getState(), myId);
                iAmDead = false;
                break;
            }
        }

        if (iAmDead)
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
        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(errorMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .build();
        messageSender.sendAck(ack, sender);
    }

    public void changeDirection(SnakeProto.Direction direction) throws IOException {

        if (myRole != SnakeProto.NodeRole.MASTER){
            SnakeProto.GameMessage.SteerMsg steerMsg = SnakeProto.GameMessage.SteerMsg.newBuilder()
                    .setDirection(direction)
                    .build();

            SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                    .setSteer(steerMsg)
                    .setMsgSeq(getMsgSeq())
                    .build();
            messageSender.sendMessage(gameMessage, serverAddress);
        }
        else{
            model.changeDirection(myId, direction);
        }
    }

    /*Functions for MASTER role*/

    private void becomeMaster() throws IOException {
        //init model
        myRole = SnakeProto.NodeRole.MASTER;
        if (lastReceivedState == null){
            model = new SnakeModel(config);
        }
        else {
            for (SnakeProto.GamePlayer player: lastReceivedState.getPlayers().getPlayersList()){
                players.put(player.getId(), player);
            }
            SnakeProto.GamePlayer myNewPlayer = players.get(myId).toBuilder().clone().setRole(SnakeProto.NodeRole.MASTER).build();
            players.remove(myId);
            players.put(myId, myNewPlayer);

            model = new SnakeModel(lastReceivedState.toBuilder().clone().setPlayers(SnakeProto.GamePlayers.newBuilder().addAllPlayers(players.values())).build());

            //pick deputy
            deputyAddress = null;
            for (SnakeProto.GamePlayer player: players.values()){
                InetSocketAddress playerAddress = new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort());
                if (!playerAddress.equals(serverAddress) && player.getId() != myId){
                    pickDeputy(playerAddress);
                    break;
                }

            }
        }

        new Thread(this::stateControlLoop).start();

        new Thread(this::sendAnnouncementMsgLoop).start();
    }

    private int getId(){
        return (int) System.nanoTime();
    }

    private void sendState() throws IOException {
        SnakeProto.GameState state = model.getGameState();

        snakeNodeListener.updateState(state, myId);
        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setState(SnakeProto.GameMessage.StateMsg.newBuilder().setState(state))
                .setMsgSeq(getMsgSeq())
                .build();
        for (SnakeProto.GamePlayer player: players.values()){
            if (player.getId() != myId){
                messageSender.sendMessage(gameMessage, new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
            }
        }
    }

    private void sendAnnouncementMsgLoop(){
        while (!Thread.currentThread().isInterrupted()){

            //create announcement message
            SnakeProto.GameMessage.AnnouncementMsg announcementMsg = SnakeProto.GameMessage.AnnouncementMsg.newBuilder()
                    .setPlayers(SnakeProto.GamePlayers.newBuilder().addAllPlayers(players.values()).build())
                    .setConfig(config)
                    .build();
            //send message
            try {
                byte[] announcementMessageBytes = announcementMsg.toByteArray();
                InetAddress announcementMsgAddress = InetAddress.getByName(NetConfig.ANNOUNCEMENT_MSG_ADDRESS);
                DatagramPacket packet = new DatagramPacket(announcementMessageBytes, announcementMessageBytes.length, announcementMsgAddress, NetConfig.ANNOUNCEMENT_MSG_PORT);
                socket.send(packet);

            } catch (IOException e) {
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
        for (SnakeProto.GamePlayer player: players.values()){
            if (player.getIpAddress().equals(sender.getAddress().getHostAddress()) && player.getPort() == sender.getPort()){
                model.changeDirection(player.getId(), steerMsg.getSteer().getDirection());
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

    private void pickDeputy(InetSocketAddress sender) throws IOException {
        System.out.println("Picking " + sender.toString() +" as deputy");
        deputyAddress = sender;
        SnakeProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakeProto.GameMessage.RoleChangeMsg.newBuilder()
                .setReceiverRole(SnakeProto.NodeRole.DEPUTY)
                .setSenderRole(SnakeProto.NodeRole.MASTER)
                .build();
        SnakeProto.GameMessage gameMessage = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(getMsgSeq())
                .setRoleChange(roleChangeMsg)
                .setSenderId(myId)
                .build();
        messageSender.sendMessage(gameMessage, sender);
    }

    private void handleJoinMsg(SnakeProto.GameMessage joinMsg, InetSocketAddress sender) throws IOException {
        System.out.println("Got join form " + sender.toString());

        //create new player
        SnakeProto.GamePlayer player = SnakeProto.GamePlayer.newBuilder()
                .setName(joinMsg.getJoin().getName())
                .setId(getId())
                .setIpAddress(sender.getAddress().getHostAddress())
                .setPort(sender.getPort())
                .setRole(deputyAddress == null? SnakeProto.NodeRole.DEPUTY : SnakeProto.NodeRole.NORMAL)
                .setType(SnakeProto.PlayerType.HUMAN)
                .setScore(0)
                .build();



        model.addPlayer(player);
        players.put(player.getId(), player);

        //send ack
        SnakeProto.GameMessage ack = SnakeProto.GameMessage.newBuilder()
                .setMsgSeq(joinMsg.getMsgSeq())
                .setAck(SnakeProto.GameMessage.AckMsg.newBuilder().build())
                .setReceiverId(player.getId())
                .build();
        messageSender.sendAck(ack, sender);

        if (deputyAddress == null) {
            pickDeputy(sender);
        }
    }

    @Override
    public void run() {
        new Thread(messageSender).start();
        new Thread(this::sendPingLoop).start();

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
            try {
                becomeMaster();
            } catch (IOException e) {
                return;
            }
            try {
                SnakeProto.GamePlayer myPlayer = SnakeProto.GamePlayer.newBuilder()
                        .setName(name)
                        .setId(getId())
                        .setIpAddress(InetAddress.getLocalHost().getHostAddress())
                        .setPort(socket.getLocalPort())
                        .setRole(SnakeProto.NodeRole.MASTER)
                        .setScore(0)
                        .build();
                myId = myPlayer.getId();
                model.addPlayer(myPlayer);
                players.put(myId, myPlayer);
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
                return;
            }
        }

    }

    @Override
    public void connectionNotResponding(InetSocketAddress address) {
        System.out.println(address.toString() + " not responding");
        messageSender.stopWaiting(address);
        try {
            if (myRole == SnakeProto.NodeRole.NORMAL && address.equals(serverAddress)){
                if (deputyAddress != null)
                    serverAddress = deputyAddress;
                else
                    System.out.println("Server is dead, but I have no deputy :(");
            }
            else if (myRole == SnakeProto.NodeRole.MASTER){
                if (address.equals(deputyAddress)){

                        //remove player
                        for (SnakeProto.GamePlayer player: players.values()){
                            if (new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()).equals(address)){
                                players.remove(player.getId());
                                break;
                            }
                        }

                        //pick new deputy
                        if (deputyAddress == address){
                            deputyAddress = null;
                            for (SnakeProto.GamePlayer player: players.values()){
                                if (player.getId() != myId){
                                    pickDeputy(new InetSocketAddress(InetAddress.getByName(player.getIpAddress()), player.getPort()));
                                    break;
                                }
                            }
                        }
                }
            }
            else if (myRole == SnakeProto.NodeRole.DEPUTY && address.equals(serverAddress)){
                System.out.println("Becoming master");
                becomeMaster();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
