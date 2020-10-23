import com.sun.deploy.util.SessionState;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerMain {
    public static void main(String[] args) {
        if (args.length != 1){
            System.out.println("Usage: <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            new Server(serverSocket).start();
            System.out.println("Server started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
