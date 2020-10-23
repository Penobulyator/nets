import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
    }

    public void sendFile(File file) throws IOException {
        new FileSender(socket).sendFile(file);
    }
}
