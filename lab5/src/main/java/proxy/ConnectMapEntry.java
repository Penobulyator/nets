package proxy;

import lombok.Getter;

import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

public class ConnectMapEntry {

    @Getter
    SocketChannel connectableSocket;

    @Getter
    DatagramChannel dnsSocket;

    @Getter
    String hostName;

    @Getter
    int port;

    public ConnectMapEntry(SocketChannel connectableSocket, DatagramChannel dnsSocket, String hostName, int port) {
        this.connectableSocket = connectableSocket;
        this.dnsSocket = dnsSocket;
        this.hostName = hostName;
        this.port = port;
    }
}
