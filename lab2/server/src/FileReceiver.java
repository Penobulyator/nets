import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class FileReceiver extends Thread{
    private Socket socket;
    private long receivedBytesCount = 0;
    private static final String UPLOAD_DIR = "uploads";

    public FileReceiver(Socket client){
        this.socket = client;
    }

    public String getClientAddress(){
        return socket.getInetAddress().toString();
    }

    public int getClientPort(){
        return socket.getPort();
    }

    public long getReceivedBytesCount() {
        return receivedBytesCount;
    }

    private File createNewFile(String filename) throws IOException {

        File file = new File(UPLOAD_DIR, filename);
        int attempt = 0;
        String[] splitName = filename.split("\\.", 2);
        String name = splitName[0];
        String extension = splitName.length > 1? splitName[1] : "";
        while (!file.createNewFile()){
            file = new File(UPLOAD_DIR, name + attempt + "." + extension);
            attempt++;
        }
        return file;
    }

    private void receiveFile(String filename, long fileSize) throws IOException {
        File file = createNewFile(filename);
        FileOutputStream out = new FileOutputStream(file);

        byte[] buf = new byte[Protocol.PART_LENGTH];
        while(true){
            int length;
            try {
                length = socket.getInputStream().read(buf);
            }catch (IOException ex){
                out.flush();
                out.close();
                return;
            }
            out.write(buf, 0, length);
            receivedBytesCount += length;
            if (receivedBytesCount == fileSize){
                out.flush();
                out.close();
                socket.getOutputStream().write(Protocol.GOOD_RESPONSE.getBytes());
                return;
            }
            else if (receivedBytesCount > fileSize){
                socket.getOutputStream().write(Protocol.BAD_RESPONSE.getBytes());
            }
        }
    }

    @Override
    public void run(){
        try {
            try {
                //receive header
                FileHeader fileHeader = FileHeader.read(socket);

                //receive file
                receiveFile(fileHeader.getFilename(), fileHeader.getFileSize());
            } catch (IOException e) {
                try {
                    socket.getOutputStream().write(Protocol.GOOD_RESPONSE.getBytes());
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
        finally {
            try {
                socket.close();
            }catch (IOException ex){
                System.out.println(ex.getMessage());
            }
        }

    }
}
