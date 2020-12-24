package proxy;

import proxy.socketHandler.clientSocketHandler.ClientSocketHandler;
import proxy.socketHandler.clientSocketHandler.ClientSocketHandlerListener;
import proxy.socketHandler.serverSocketHandler.HostSocketHandler;
import proxy.socketHandler.serverSocketHandler.HostSocketHandlerListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
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
                if (!selectionKey.isValid())
                    continue;

                if (selectionKey.isAcceptable()){
                    accept();
                }
                else {

                    SelectableChannel channel = selectionKey.channel();

                    /*if (selectionKey.isConnectable() && (selectionKey.interestOps() & SelectionKey.OP_CONNECT) != 0){
                        for (ProxyEntry entry: proxyEntries){
                            if (entry.hasHostHandler()) {
                                SocketChannel hostSocket = entry.getHostSocket();

                                if (hostSocket.equals(channel)){
                                    if (hostSocket.finishConnect()){
                                        System.out.println("Connected to " + hostSocket.getRemoteAddress().toString());
                                        selectionKey.interestOps(0);
                                        selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                    }
                                    else {
                                        System.out.println("Failed to connect to " + hostSocket.getRemoteAddress().toString());
                                        closeSession(hostSocket);
                                    }
                                    break;
                                }
                            }
                        }
                    }*/

                    for (ProxyEntry proxyEntry: proxyEntries) {
                        if (channel.equals(proxyEntry.getClientSocket()) || channel.equals(proxyEntry.getHostSocket())){
                            proxyEntry.getClientSocketHandler().handle(selectionKey);

                            if (proxyEntry.hasHostHandler() && proxyEntry.getHostSocket().isConnected()){
                                proxyEntry.getHostSocketHandler().handle(selectionKey);
                            }

                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void addServerSocketListener(SocketChannel hostSocket, SocketChannel clientSocket, InetSocketAddress hostAddress) {
        try {
            //TODO: make resolve and connect nonblocking
            //hostSocket.configureBlocking(false);
            //hostSocket.connect(hostAddress);
            //System.out.println("Stating to connect to " + hostAddress + " (requested by " + clientSocket.getRemoteAddress() + ")");
            //hostSocket.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            hostSocket.connect(hostAddress);
            hostSocket.configureBlocking(false);
            hostSocket.register(selector,  SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            for (ProxyEntry proxyEntry: proxyEntries){
                if (proxyEntry.getClientSocket().equals(clientSocket)){
                    proxyEntry.addServerSocketHandler(hostSocket, new HostSocketHandler(hostSocket, clientSocket, this));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeSession(SocketChannel socketChannel) {
        for (ProxyEntry proxyEntry: proxyEntries){
            if (socketChannel.equals(proxyEntry.getClientSocket()) || socketChannel.equals(proxyEntry.getHostSocket())){
                try {
                    //System.out.println("Closing session between " + proxyEntry.getClientSocket().getRemoteAddress() + " and " + proxyEntry.getHostSocket().getRemoteAddress());
                    proxyEntry.getClientSocket().close();

                    if (proxyEntry.hasHostHandler()){
                        proxyEntry.getHostSocket().close();
                    }
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
