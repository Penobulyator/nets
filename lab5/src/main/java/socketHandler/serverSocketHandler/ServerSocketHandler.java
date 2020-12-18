package socketHandler.serverSocketHandler;

import socketHandler.messageForwarder.MessageForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

public class ServerSocketHandler {
    SocketChannel readSocketChanel;
    SocketChannel writeSocketChannel;

    MessageForwarder forwarder;

    ServerSocketHandlerListener listener;

    public ServerSocketHandler(SocketChannel readSocketChanel, SocketChannel writeSocketChannel, ServerSocketHandlerListener listener) {
        this.readSocketChanel = readSocketChanel;
        this.writeSocketChannel = writeSocketChannel;
        this.listener = listener;

        forwarder = new MessageForwarder(readSocketChanel, writeSocketChannel);
    }

    public void handle(SelectionKey selectionKey) throws IOException {
        if (selectionKey.channel().equals(readSocketChanel) && selectionKey.isReadable()){
            boolean success = forwarder.read();
            if (!success){
                listener.closeSocket(readSocketChanel);
            }
        }
        else if (selectionKey.channel().equals(writeSocketChannel) && selectionKey.isWritable()){
            boolean success = forwarder.write();
            if (!success){
                listener.closeSocket(writeSocketChannel);
            }
        }
    }
}
