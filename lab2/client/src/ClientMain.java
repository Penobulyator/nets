import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.*;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length != 3){
            System.out.println("Usage: <address> <port> <file path>");
            return;
        }

        Socket socket = null;
        try {
            InetAddress address = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);
            String path = args[2];
            socket =  new Socket(address, port);
            socket.setReuseAddress(true);

            Client client = new Client(socket);
            client.sendFile(new File(path));

        } catch (IOException e) {
            if (socket != null)
                try {
                    socket.close();
                }catch (IOException ex){
                    ex.printStackTrace();
                }
            System.out.println(e.getMessage());
        }
    }
}
