import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {
    private static void printUsage(){
        System.out.println("Usage: <group_address> <port>");
    }
    public static void main(String[] args) {
        if (args.length < 2){
            printUsage();
            return;
        }

        final int port = Integer.parseInt(args[1]);
        final InetAddress group;
        try {
            group = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("Bad group address");
            return;
        }

        try {
            MulticastReceiverListener listener = new MulticastReceiverListener();
            MulticastSender sender = new MulticastSender(group);
            MulticastReceiver receiver = new MulticastReceiver(group, port, listener);

            listener.start();
            sender.start();
            receiver.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                listener.interrupt();
                sender.interrupt();
                receiver.interrupt();

                try {
                    listener.join();
                    sender.join();
                    receiver.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
