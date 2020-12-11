package model.snakeNode.announcementMessageReceiver;

import model.snakeProto.SnakeProto;

import java.net.InetSocketAddress;

public interface AnnouncementMessageReceiverListener {
    void gotAnnouncementMessage(InetSocketAddress serverAddress, SnakeProto.GameMessage.AnnouncementMsg announcementMsg);
    void announcementTimeout(InetSocketAddress serverAddress);
}
