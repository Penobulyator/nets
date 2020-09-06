import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class MulticastReceiver extends Thread{
    private MulticastSocket rxSocket;
    private InetAddress group;
    private int port;

    public Set<InetAddress> getConnections() {
        return connections;
    }

    private Set<InetAddress> connections = new HashSet<>();

    public MulticastReceiver(int port, InetAddress group) throws IOException {
        this.port = port;
        this.group = group;

        rxSocket = new MulticastSocket(port);
        rxSocket.joinGroup(group);
    }

    @Override
    public void run() {
        while(!currentThread().isInterrupted()){
            connections.clear();
            byte[]buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                rxSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                //Do something??
            }
            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            if (receivedMessage.equals(Protocol.message)){
                connections.add(packet.getAddress());
            }
        }
        rxSocket.close();
    }
}
