import java.io.*;
import java.net.Socket;
public class FileSender {
    Socket socket;

    public FileSender(Socket socket) {
        this.socket = socket;
    }

    private void sendFileHeader(File file) throws IOException {
        FileHeader header = new FileHeader(file);
        header.write(socket);
    }

    private boolean sendIsSuccessful() throws IOException {
        byte[] buf = new byte[Math.max(Protocol.BAD_RESPONSE.length(), Protocol.GOOD_RESPONSE.length())];
        int length = socket.getInputStream().read(buf);
        String response = new String(buf, 0 ,length);
        if (response.equals(Protocol.GOOD_RESPONSE))
            return true;
        else if (response.equals(Protocol.BAD_RESPONSE))
            return false;
        else{
            System.out.println("Unknown response");
            return false;
        }
    }

    public void sendFile(File file) throws IOException {
        sendFileHeader(file);
        long fileSize = file.length();
        FileInputStream in = null;
        try{
            in = new FileInputStream(file);
            long sentBytesCount = 0;
            byte[] buf = new byte[Protocol.PART_LENGTH];

            while(true){
                int length = in.read(buf, 0, Protocol.PART_LENGTH);
                sentBytesCount += length;
                socket.getOutputStream().write(buf, 0, length);
                if (sentBytesCount >= fileSize){
                    if (sendIsSuccessful())
                        System.out.println("Sent successfully");
                    else
                        System.out.println("Sent unsuccessfully");
                    return;
                }
            }
        }
        finally {
            if (in != null)
                in.close();
        }


    }
}
