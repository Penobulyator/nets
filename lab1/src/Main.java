import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {
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
    private static void printUsage(){
        System.out.println("Usage: group_address port");
    }
    public static void main(String[] args) {
        if (args.length < 2){
            printUsage();
            return;
        }

        final InetAddress group;
        try {
            group = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("Bad group address");
            return;
        }
        final int port = Integer.parseInt(args[1]);

        try {
            MulticastSender sender = new MulticastSender(group, port);
            MulticastReceiver receiver = new MulticastReceiver(group, port);

            sender.start();
            receiver.start();

            listenToReceiver(receiver);
            Thread.sleep(1000000000);
            sender.interrupt();
            //receiver.interrupt();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
