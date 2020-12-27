package proxy.socketHandler.clientSocketHandler;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public interface ClientSocketHandlerListener {
    void addHostSocket(SocketChannel hostSocket, SocketChannel clientSocket, String hostName, int port);
    void closeSession(SocketChannel socketChannel);
}
