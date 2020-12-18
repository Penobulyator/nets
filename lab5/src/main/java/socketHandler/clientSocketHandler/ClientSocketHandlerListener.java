package socketHandler.clientSocketHandler;

import java.nio.channels.SocketChannel;

public interface ClientSocketHandlerListener {
    void addServerSocketListener(SocketChannel readSocketChanel, SocketChannel writeSocketChanel);
    void closeSocket(SocketChannel socketChannel);
}
