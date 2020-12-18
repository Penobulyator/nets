package proxy;

import proxy.socketHandler.clientSocketHandler.ClientSocketHandler;
import proxy.socketHandler.clientSocketHandler.ClientSocketHandlerListener;
import proxy.socketHandler.serverSocketHandler.HostSocketHandler;
import proxy.socketHandler.serverSocketHandler.HostSocketHandlerListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SocksProxy implements ClientSocketHandlerListener, HostSocketHandlerListener {
    ServerSocketChannel serverSocketChannel;

    Selector selector;

    Set<ProxyEntry> proxyEntries = ConcurrentHashMap.newKeySet();

    public SocksProxy(ServerSocketChannel serverSocketChannel) throws IOException {
        this.serverSocketChannel = serverSocketChannel;
        selector = Selector.open();
    }

    private void accept() throws IOException {
        SocketChannel clientSocket = serverSocketChannel.accept();

        if (clientSocket != null){
            clientSocket.configureBlocking(false);
            clientSocket.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            proxyEntries.add(new ProxyEntry(clientSocket, new ClientSocketHandler(clientSocket, this)));

            //System.out.println("Accepted " + clientSocket.getRemoteAddress().toString());
        }
    }

    public void start() throws IOException {

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            selector.select();
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            for (SelectionKey selectionKey: selectionKeySet){

                if (selectionKey.isValid() && selectionKey.isAcceptable()){
                    accept();
                }
                else {

                    SelectableChannel channel = selectionKey.channel();

                    for (ProxyEntry proxyEntry: proxyEntries) {
                        if (channel.equals(proxyEntry.getClientSocket()) || channel.equals(proxyEntry.getHostSocket())){
                            proxyEntry.getClientSocketHandler().handle(selectionKey);

                            if (proxyEntry.hasHostHandler()){
                                proxyEntry.getHostSocketHandler().handle(selectionKey);
                            }
                        }
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

            for (ProxyEntry proxyEntry: proxyEntries){
                if (proxyEntry.getClientSocket().equals(writeSocketChanel)){
                    proxyEntry.addServerSocketHandler(readSocketChanel, new HostSocketHandler(readSocketChanel, writeSocketChanel, this));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeSocket(SocketChannel socketChannel) {
        for (ProxyEntry proxyEntry: proxyEntries){
            if (socketChannel.equals(proxyEntry.getClientSocket()) || socketChannel.equals(proxyEntry.getHostSocket())){
                try {
                    proxyEntry.getClientSocket().close();

                    if (proxyEntry.hasHostHandler())
                        proxyEntry.getHostSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                proxyEntries.remove(proxyEntry);
                break;
            }
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
