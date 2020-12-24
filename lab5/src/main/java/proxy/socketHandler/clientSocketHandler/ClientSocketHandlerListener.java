package proxy.socketHandler.clientSocketHandler;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public interface ClientSocketHandlerListener {
    void addServerSocketListener(SocketChannel hostSocket, SocketChannel clientSocket, InetSocketAddress hostAddress);
    void closeSession(SocketChannel socketChannel);
}
