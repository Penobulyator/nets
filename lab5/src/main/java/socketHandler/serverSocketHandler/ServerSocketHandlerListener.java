package socketHandler.serverSocketHandler;

import java.nio.channels.SocketChannel;

public interface ServerSocketHandlerListener {
    void closeSocket(SocketChannel socketChannel);
}
