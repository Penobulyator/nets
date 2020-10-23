import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

public class Server {
    ServerSocket serverSocket;


    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void start(){
        ConnectionSpeedCounter speedCounter = new ConnectionSpeedCounter();
        speedCounter.start();
        while(true){
            try {
                Socket client = serverSocket.accept();
                FileReceiver receiver = new FileReceiver(client);
                receiver.start();
                speedCounter.addReceiver(receiver);
            } catch (IOException e) {
                return;
            }
        }
    }
}
