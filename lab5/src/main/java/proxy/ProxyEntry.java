package proxy;

import lombok.Getter;
import lombok.Data;
import proxy.socketHandler.clientSocketHandler.ClientSocketHandler;
import proxy.socketHandler.serverSocketHandler.HostSocketHandler;

import java.nio.channels.SocketChannel;

public class ProxyEntry {
    @Getter
    private SocketChannel clientSocket;

    @Getter
    private SocketChannel hostSocket = null;

    @Getter
    private ClientSocketHandler clientSocketHandler;

    @Getter
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

}
