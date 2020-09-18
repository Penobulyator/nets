import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class MulticastReceiver extends Thread{
    private MulticastSocket rxSocket;
    private MulticastReceiverListener listener;
    private int port;
    public MulticastReceiver(InetAddress group, int port, MulticastReceiverListener listener) throws IOException {
        this.listener = listener;
        this.port = port;
        this.rxSocket = new MulticastSocket(port);
        this.rxSocket.joinGroup(group);
    }

    @Override
    public void run() {
        while(!currentThread().isInterrupted()){
            byte[]buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                rxSocket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            if (receivedMessage.equals(Protocol.message) && packet.getPort() == port){
                listener.addConnection(packet.getAddress());
            }
        }
        rxSocket.close();
    }
}
