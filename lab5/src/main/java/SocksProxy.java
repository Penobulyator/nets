import socketHandler.clientSocketHandler.ClientSocketHandler;
import socketHandler.clientSocketHandler.ClientSocketHandlerListener;
import socketHandler.serverSocketHandler.ServerSocketHandler;
import socketHandler.serverSocketHandler.ServerSocketHandlerListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SocksProxy implements ClientSocketHandlerListener, ServerSocketHandlerListener {
    ServerSocketChannel serverSocketChannel;

    Selector selector;

    Map<SocketChannel , ClientSocketHandler> clientSocketHandlersReadChannels = new HashMap<>();
    Map<SocketChannel , ClientSocketHandler> clientSocketHandlersWriteChannels = new HashMap<>();

    Map<SocketChannel, ServerSocketHandler> serverSocketHandlersReadChannels = new HashMap<>();
    Map<SocketChannel, ServerSocketHandler> serverSocketHandlersWriteChannels = new HashMap<>();

    public SocksProxy(ServerSocketChannel serverSocketChannel) throws IOException {
        this.serverSocketChannel = serverSocketChannel;
        selector = Selector.open();
    }

    private void accept() throws IOException {
        SocketChannel clientSocket = serverSocketChannel.accept();

        if (clientSocket != null){
            clientSocket.configureBlocking(false);
            clientSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            clientSocketHandlersReadChannels.put(clientSocket, new ClientSocketHandler(clientSocket, this));

            //System.out.println("Accepted " + clientSocket.getRemoteAddress().toString());
        }
    }

    public void start() throws IOException {

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            selector.select();
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            for (SelectionKey selectionKey: selectionKeySet){

                if (selectionKey.isAcceptable() && selectionKey.channel().equals(serverSocketChannel)){
                    accept();
                }
                else {

                    SelectableChannel channel = selectionKey.channel();

                    if (clientSocketHandlersReadChannels.containsKey(channel)){
                        clientSocketHandlersReadChannels.get(channel).handle(selectionKey);
                    }

                    if (clientSocketHandlersWriteChannels.containsKey(channel)){
                        clientSocketHandlersWriteChannels.get(channel).handle(selectionKey);
                    }

                    if (serverSocketHandlersReadChannels.containsKey(channel)){
                        serverSocketHandlersReadChannels.get(channel).handle(selectionKey);
                    }

                    if (serverSocketHandlersWriteChannels.containsKey(channel)){
                        serverSocketHandlersWriteChannels.get(channel).handle(selectionKey);
                    }
                }
            }
        }
    }

    @Override
    public void addServerSocketListener(SocketChannel readSocketChanel, SocketChannel writeSocketChanel) {
        try {
            readSocketChanel.configureBlocking(false);
            readSocketChanel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            ServerSocketHandler serverSocketHandler = new ServerSocketHandler(readSocketChanel, writeSocketChanel, this);

            clientSocketHandlersWriteChannels.put(readSocketChanel, clientSocketHandlersReadChannels.get(writeSocketChanel));

            serverSocketHandlersReadChannels.put(readSocketChanel, serverSocketHandler);
            serverSocketHandlersWriteChannels.put(writeSocketChanel, serverSocketHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeSocket(SocketChannel socketChannel) {
        clientSocketHandlersReadChannels.remove(socketChannel);
        clientSocketHandlersWriteChannels.remove(socketChannel);
        serverSocketHandlersReadChannels.remove(socketChannel);
        serverSocketHandlersWriteChannels.remove(socketChannel);

        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 10000;

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", port));
        serverSocketChannel.configureBlocking(false);

        SocksProxy proxy = new SocksProxy(serverSocketChannel);
        proxy.start();
    }

}
