import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;

public class MulticastSender extends Thread{
    private MulticastSocket txSocket;
    private InetAddress group;
    private int port;


    public MulticastSender(int port, InetAddress group) throws IOException {
        this.port = port;
        this.group = group;

        txSocket = new MulticastSocket(port);
        txSocket.joinGroup(group);
    }

    @Override
    public void run() {
        while (Thread.currentThread().isInterrupted()){
            DatagramPacket packet = new DatagramPacket(Protocol.message.getBytes(), Protocol.message.length(), group, port);
            try {
                txSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                //Do something??
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
