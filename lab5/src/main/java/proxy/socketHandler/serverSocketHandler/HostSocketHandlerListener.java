package proxy.socketHandler.serverSocketHandler;

import java.nio.channels.SocketChannel;

public interface HostSocketHandlerListener {
    void closeSession(SocketChannel socketChannel);
}
