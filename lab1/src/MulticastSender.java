import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;

public class MulticastSender extends Thread{
    private DatagramSocket txSocket;
    private InetAddress address;
    private int receiversPort;

    public MulticastSender(InetAddress address) throws IOException {
        this.address = address;
        this.txSocket = new DatagramSocket();
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()){
            DatagramPacket packet = new DatagramPacket(Protocol.message.getBytes(), Protocol.message.length(), address, receiversPort);
            try {
                txSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                sleep(Protocol.resendTime * 1000);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
        txSocket.close();
    }
}
