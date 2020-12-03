package model.client.announcementMessageReceiver;

import model.netConfig.NetConfig;
import model.snakeProto.SnakeProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnouncementMessageReceiver implements Runnable {
    private Map<InetSocketAddress, Integer> silenceTimeMap = new ConcurrentHashMap<>();

    public AnnouncementMessageReceiver(MulticastSocket socket, AnnouncementMessageReceiverListener listener) {
        this.socket = socket;
        this.listener = listener;
    }

    private MulticastSocket socket;

    private AnnouncementMessageReceiverListener listener;

    private static final int TIMEOUT_CHECK_PERIOD = NetConfig.ANNOUNCEMENT_MSG_RESEND_TIME / 5;

    void checkAnnouncementMsgTimeout(){
        while (!Thread.currentThread().isInterrupted()){
            for (Map.Entry<InetSocketAddress, Integer> silenceTimeMapEntry: silenceTimeMap.entrySet()){
                if (silenceTimeMapEntry.getValue() == 0){
                    listener.announcementTimeout(silenceTimeMapEntry.getKey());
                    silenceTimeMap.remove(silenceTimeMapEntry.getKey());
                }
                else
                    silenceTimeMap.put(silenceTimeMapEntry.getKey(), silenceTimeMapEntry.getValue() - TIMEOUT_CHECK_PERIOD);
            }

            try {
                Thread.sleep(TIMEOUT_CHECK_PERIOD);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        Thread timeoutCheckingThread = new Thread(this::checkAnnouncementMsgTimeout);
        timeoutCheckingThread.start();

        while (!Thread.currentThread().isInterrupted()){
            try {
                DatagramPacket packet = new DatagramPacket(new byte[NetConfig.MAX_MSG_LENGTH], NetConfig.MAX_MSG_LENGTH);
                socket.receive(packet);

                SnakeProto.GameMessage.AnnouncementMsg announcementMsg = SnakeProto.GameMessage.AnnouncementMsg.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                InetSocketAddress sender = (InetSocketAddress) packet.getSocketAddress();

                listener.gotAnnouncementMessage(sender, announcementMsg);

                silenceTimeMap.put(sender, NetConfig.ANNOUNCEMENT_MSG_RESEND_TIME);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                timeoutCheckingThread.interrupt();
                return;
            }
        }
    }
}
