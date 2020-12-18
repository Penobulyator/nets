package proxy.socketHandler.serverSocketHandler;

import proxy.socketHandler.messageForwarder.MessageForwarder;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HostSocketHandler {
    SocketChannel readSocketChanel;
    SocketChannel writeSocketChannel;

    MessageForwarder forwarder;

    HostSocketHandlerListener listener;

    public HostSocketHandler(SocketChannel readSocketChanel, SocketChannel writeSocketChannel, HostSocketHandlerListener listener) {
        this.readSocketChanel = readSocketChanel;
        this.writeSocketChannel = writeSocketChannel;
        this.listener = listener;

        forwarder = new MessageForwarder(readSocketChanel, writeSocketChannel);
    }

    public void handle(SelectionKey selectionKey) throws IOException {
        if (!selectionKey.isValid())
            return;

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
