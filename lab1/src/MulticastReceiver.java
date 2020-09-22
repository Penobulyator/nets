import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.*;

public class MulticastReceiver extends Thread{
    private MulticastSocket rxSocket;
    private MulticastReceiverListener listener;
    public MulticastReceiver(MulticastSocket rxSocket, MulticastReceiverListener listener) throws IOException {
        this.listener = listener;
        this.rxSocket = rxSocket;
    }

    @Override
    public void run() {
        while(!currentThread().isInterrupted()){
            byte[]buf = new byte[256];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                rxSocket.receive(packet);
            } catch (IOException e) {
                return;
            }
            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            if (receivedMessage.equals(Protocol.message)){
                listener.addConnection(new Connection(packet.getAddress(), packet.getPort()));
            }
        }
    }
}
