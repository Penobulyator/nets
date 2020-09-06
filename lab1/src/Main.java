import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {
    private static final int port = 9000;
    private static final String group = "224.0.0.1";

    public static void listenToReceiver(MulticastReceiver receiver){
        Map<InetAddress, Integer> connections = new HashMap<>();
        while(!Thread.currentThread().isInterrupted()){
            //se if any connection is appeared
            Set<InetAddress> newConnections = receiver.getConnections();
            newConnections.retainAll(connections.keySet());

            Set<InetAddress> notRespondedConnections = connections.keySet();
            notRespondedConnections.retainAll(receiver.getConnections());

            //add new connections
            for (InetAddress address: newConnections){
                connections.put(address, Protocol.maxSilenceTime);
            }

            //reduce silence time for not responded connections
            for (InetAddress address: notRespondedConnections){
                if (connections.get((address)).equals(1)){
                    connections.remove(address);
                }
                else{
                    connections.replace(address, connections.get(address) - 1);
                }
            }

            //print connections
            System.out.flush();
            for (InetAddress address: connections.keySet()){
                System.out.println(address.getHostAddress());
            }

            try {
                Thread.sleep(Protocol.resendTime * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        try {
            MulticastSender sender = new MulticastSender(port, InetAddress.getByName(group));
            MulticastReceiver receiver = new MulticastReceiver(port, InetAddress.getByName(group));

            sender.start();
            receiver.start();

            listenToReceiver(receiver);

            sender.interrupt();
            receiver.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
