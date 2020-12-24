package proxy.socketHandler.serverSocketHandler;

import proxy.socketHandler.messageForwarder.MessageForwarder;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HostSocketHandler {
    SocketChannel hostSocket;
    SocketChannel clientSocket;

    MessageForwarder forwarder;

    HostSocketHandlerListener listener;

    public HostSocketHandler(SocketChannel hostSocket, SocketChannel clientSocket, HostSocketHandlerListener listener) {
        this.hostSocket = hostSocket;
        this.clientSocket = clientSocket;

        this.listener = listener;

        forwarder = new MessageForwarder(hostSocket, clientSocket);
    }

    public void handle(SelectionKey selectionKey) throws IOException {
        if (selectionKey.channel().equals(hostSocket) && selectionKey.isReadable()){
            boolean success = forwarder.tryRead();
            if (!success){
                listener.closeSession(hostSocket);
                return;
            }
        }
        else if (selectionKey.channel().equals(clientSocket) && selectionKey.isWritable()){
            boolean success = forwarder.tryWrite();
            if (!success){
                listener.closeSession(clientSocket);
                return;
            }
        }

        if (forwarder.hasMessageToForward()){
            //selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        else {
            //selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }
}
