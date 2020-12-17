package model.net.MsgSender;

import java.net.InetSocketAddress;

public interface MessageSenderListener {
    void connectionNotResponding(InetSocketAddress address);
}
