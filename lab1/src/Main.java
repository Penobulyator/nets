import java.io.IOException;
import java.net.*;
public class Main {
    private static void printUsage(){
        System.out.println("Usage: <group_address> <port>");
    }

    public static void start(InetAddress group, int port){
        try {
            //init listener
            MulticastReceiverListener listener = new MulticastReceiverListener();

            //init receiver
            MulticastSocket receiverSocket = new MulticastSocket(port);
            receiverSocket.joinGroup(group);
            MulticastReceiver receiver = new MulticastReceiver(receiverSocket, listener);

            //init sender
            DatagramSocket senderSocket = new DatagramSocket();
            MulticastSender sender = new MulticastSender(senderSocket, group, port);

            listener.start();
            sender.start();
            receiver.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                senderSocket.close();
                receiverSocket.close();

                listener.interrupt();
                sender.interrupt();
                receiver.interrupt();

                try {
                    listener.join();
                    sender.join();
                    receiver.join();
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        start(group, port);
    }
}
