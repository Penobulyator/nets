package model.MsgSender;

import model.snakeProto.SnakeProto;

import java.net.InetSocketAddress;

public class ResendInfo {
    public InetSocketAddress inetSocketAddress;
    public SnakeProto.GameMessage message;
    public int silenceTime;

    public ResendInfo(InetSocketAddress inetSocketAddress, SnakeProto.GameMessage message, int silenceTime) {
        this.inetSocketAddress = inetSocketAddress;
        this.message = message;
        this.silenceTime = silenceTime;
    }
}
