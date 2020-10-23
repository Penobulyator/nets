import java.net.*;

public class Main {
    public static void main(String[] args) throws SocketException, UnknownHostException {
        if (args.length != 3 && args.length != 5){
            System.out.println("Usage: <node name> <loss percent> <src port> opt:(<parent IP> <parentPort>)");
            return;
        }

        String name = args[0];
        int lossPercent = Integer.parseInt(args[1]);
        int srcPort = Integer.parseInt(args[2]);
        DatagramSocket socket = new DatagramSocket(srcPort);
        if (args.length == 3){
            new Thread(new ChatNode(socket, lossPercent, name)).start();
        }
        else {
            InetAddress parentIp = InetAddress.getByName(args[3]);
            int parentPort = Integer.parseInt(args[4]);
            Connection parent = new Connection(parentIp, parentPort);
            new Thread(new ChatNode(socket, lossPercent, name, parent)).start();
        }
    }
}
