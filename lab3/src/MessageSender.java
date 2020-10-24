import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSender implements Runnable {
    private final static int TIMEOUT = 10000;
    private final static int RESEND_NUMBER = 10;

    DatagramSocket socket;
    ChatNode chatNode;

    // <id, info>
    Map<UUID, ResendInfo> resendInfoMap = Collections.synchronizedMap(new ConcurrentHashMap<>());

    public MessageSender(DatagramSocket socket, ChatNode chatNode) {
        this.socket = socket;
        this.chatNode = chatNode;
    }

    private void sendMessage(UUID id) throws IOException {
        if (resendInfoMap.containsKey(id)){
            ResendInfo resendInfo = resendInfoMap.get(id);
            byte[] message = resendInfo.message.getBytes();
            Connection connection = resendInfo.connection;

            DatagramPacket packet = new DatagramPacket(message, message.length, connection.getIpAddress(), connection.getPort());
            socket.send(packet);

        }
    }

    public void sendMessage(Message message, Connection connection) throws IOException {
        resendInfoMap.put(message.getId(), new ResendInfo(connection, message, 0));
        sendMessage(message.getId());
    }

    public void sendAck(Message message, Connection connection) throws IOException {
        //System.out.println("Sending ack to " + connection.toString() + ", UUID = " + message.getId().toString());
        byte[] messageBytes = message.getBytes();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, connection.getIpAddress(), connection.getPort());
        socket.send(packet);
    }

    public void stopWaiting(Connection connection){

        for(Map.Entry<UUID, ResendInfo> entry: resendInfoMap.entrySet()){
            if (entry.getValue().connection.equals(connection)){
                resendInfoMap.remove(entry.getKey());
            }
        }
    }

    public void gotAck(UUID id){
        resendInfoMap.remove(id);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(TIMEOUT / RESEND_NUMBER);
                for(Map.Entry<UUID, ResendInfo> entry: resendInfoMap.entrySet()){
                    ResendInfo resendInfo = entry.getValue();
                    if (resendInfo.silenceTime == TIMEOUT){
                        chatNode.removeNeighbour(resendInfo.connection);
                        resendInfoMap.remove(entry.getKey());
                    }
                    else{
                        sendMessage(entry.getKey());
                        resendInfo.silenceTime += TIMEOUT / RESEND_NUMBER;
                    }
                }
            } catch (IOException | InterruptedException e) {
                return;
            }
        }
    }
}
