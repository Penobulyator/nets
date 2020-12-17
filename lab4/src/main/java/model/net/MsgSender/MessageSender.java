package model.net.MsgSender;

import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageSender implements Runnable {
    private int timeout;
    private final static int RESEND_NUMBER = 1;

    DatagramSocket socket;
    MessageSenderListener listener;

    // <id, info>
    Map<Long, ResendInfo> resendInfoMap = Collections.synchronizedMap(new ConcurrentHashMap<>());

    public MessageSender(DatagramSocket socket, MessageSenderListener listener, int timeout) {
        this.socket = socket;
        this.listener = listener;
        this.timeout = timeout;
    }

    private void sendMessage(Long id) throws IOException {
        if (resendInfoMap.containsKey(id)){
            ResendInfo resendInfo = resendInfoMap.get(id);
            byte[] message = resendInfo.message.toByteArray();
            InetSocketAddress inetSocketAddress = resendInfo.inetSocketAddress;

            DatagramPacket packet = new DatagramPacket(message, message.length, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
            socket.send(packet);
        }
    }

    public void sendMessage(SnakeProto.GameMessage message, InetSocketAddress inetSocketAddress) throws IOException {
        resendInfoMap.put(message.getMsgSeq(), new ResendInfo(inetSocketAddress, message, 0));
        sendMessage(message.getMsgSeq());
    }

    public void sendAck(SnakeProto.GameMessage ack, InetSocketAddress inetSocketAddress) throws IOException {
        byte[] messageBytes = ack.toByteArray();
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, inetSocketAddress.getAddress(), inetSocketAddress.getPort());
        socket.send(packet);
    }

    public void stopWaiting(InetSocketAddress inetSocketAddress){
        for(Map.Entry<Long, ResendInfo> entry: resendInfoMap.entrySet()){
            if (entry.getValue().inetSocketAddress.equals(inetSocketAddress)){
                resendInfoMap.remove(entry.getKey());
            }
        }
    }

    public void gotAck(Long id){
        resendInfoMap.remove(id);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(timeout / RESEND_NUMBER);
                for(Map.Entry<Long, ResendInfo> entry: resendInfoMap.entrySet()){
                    ResendInfo resendInfo = entry.getValue();
                    if (resendInfo.silenceTime == timeout){
                        listener.connectionNotResponding(resendInfo.inetSocketAddress);
                        resendInfoMap.remove(entry.getKey());
                    }
                    else{
                        sendMessage(entry.getKey());
                        resendInfo.silenceTime += timeout / RESEND_NUMBER;
                    }
                }
            } catch (IOException | InterruptedException e) {
                return;
            }
        }
    }
}
