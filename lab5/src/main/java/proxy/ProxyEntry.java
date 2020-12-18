package proxy;

import proxy.socketHandler.clientSocketHandler.ClientSocketHandler;
import proxy.socketHandler.serverSocketHandler.HostSocketHandler;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ProxyEntry {
    private SocketChannel clientSocket;
    private SocketChannel hostSocket = null;

    private ClientSocketHandler clientSocketHandler;
    private HostSocketHandler hostSocketHandler = null;

    public ProxyEntry(SocketChannel clientSocket, ClientSocketHandler clientSocketHandler) {
        this.clientSocket = clientSocket;
        this.clientSocketHandler = clientSocketHandler;
    }

    public boolean hasHostHandler(){
        return hostSocketHandler != null;
    }

    public void addServerSocketHandler(SocketChannel hostSocket, HostSocketHandler hostSocketHandler){
        this.hostSocket = hostSocket;
        this.hostSocketHandler = hostSocketHandler;
    }

    public SocketChannel getClientSocket() {
        return clientSocket;
    }

    public SocketChannel getHostSocket() {
        return hostSocket;
    }

    public ClientSocketHandler getClientSocketHandler() {
        return clientSocketHandler;
    }

    public HostSocketHandler getHostSocketHandler() {
        return hostSocketHandler;
    }

}
