import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class MulticastReceiver extends Thread{
    private MulticastSocket rxSocket;
    //private InetAddress group;
    private int port;
    public Set<InetAddress> getConnections() {
        return connections;
    }

    private Set<InetAddress> connections = new HashSet<>();

    public MulticastReceiver(InetAddress group, int port) throws IOException {
        //this.group = group;
        this.port = port;

        this.rxSocket = new MulticastSocket(port);
        this.rxSocket.joinGroup(group);
    }

    @Override
    public void run() {
        System.out.println("Receiver started, listening on port " + port);
        while(!currentThread().isInterrupted()){
            byte[]buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                rxSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
                //Do something??
            }
            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received packet from " + packet.getAddress().toString() + ":" + packet.getPort());
            if (receivedMessage.equals(Protocol.message)){
                connections.add(packet.getAddress());



            }
        }
        rxSocket.close();
        System.out.println("Receiver stopped");
    }
}
