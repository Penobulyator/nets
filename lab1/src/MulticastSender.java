import java.io.IOException;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class MulticastSender extends Thread{
    private DatagramSocket txSocket;
    private InetAddress destAddress;
    private int destPort;

    public MulticastSender(DatagramSocket txSocket, InetAddress destAddress, int destPort){
        this.destPort = destPort;
        this.destAddress = destAddress;
        this.txSocket = txSocket;
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(Protocol.message.getBytes(), Protocol.message.length(), destAddress, destPort);
            try {
                txSocket.send(packet);
            } catch (IOException e) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(Protocol.resendTime);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
