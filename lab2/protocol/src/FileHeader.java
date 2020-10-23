import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.FileHandler;

public class FileHeader {
    private final static int SIZE_OF_INT = Integer.SIZE / 8;
    private final static int SIZE_OF_LONG = Long.SIZE / 8;


    private String filename;
    private long fileSize;


    public FileHeader(String filename, long fileSize){
        this.filename = filename;
        this.fileSize = fileSize;
    }

    public FileHeader(File file){
        this.filename = file.getName();
        this.fileSize = file.length();
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() {
        return fileSize;
    }

    //header message: <int: filename size> <string: filename> <long: file size>
    public void write(Socket socket) throws IOException {
        int headerSize = SIZE_OF_INT + filename.length() + SIZE_OF_LONG;
        byte[] buf = new byte[headerSize];

        //fill name size
        ByteBuffer intBuffer = ByteBuffer.allocate(SIZE_OF_INT);
        intBuffer.putInt(filename.length());
        System.arraycopy(intBuffer.array(), 0, buf, 0, SIZE_OF_INT);

        //fill name
        System.arraycopy(filename.getBytes(), 0, buf, SIZE_OF_INT, filename.length());

        //fill file size
        ByteBuffer longBuffer = ByteBuffer.allocate(SIZE_OF_LONG);
        longBuffer.putLong(fileSize);
        System.arraycopy(longBuffer.array(), 0, buf, SIZE_OF_INT + filename.length() , SIZE_OF_LONG);

        //send
        socket.getOutputStream().write(buf);
        socket.getOutputStream().flush();
    }

    public static FileHeader read(Socket socket) throws IOException {
        //read file name size
        byte[] intBuf = new byte[SIZE_OF_INT];
        int length = socket.getInputStream().read(intBuf, 0, 4);
        if (length < SIZE_OF_INT)
            throw new IOException("bad header");
        int fileNameSize = ByteBuffer.wrap(intBuf, 0, SIZE_OF_INT).getInt();

        //read file name and file size
        byte[] header = new byte[fileNameSize + SIZE_OF_LONG];
        length = socket.getInputStream().read(header, 0, fileNameSize + SIZE_OF_LONG);
        if (length < fileNameSize + SIZE_OF_LONG)
            throw new IOException("bad header");


        String filename = new String(header, 0, fileNameSize);
        long fileSize = ByteBuffer.wrap(header, fileNameSize, SIZE_OF_LONG).getLong();

        return new FileHeader(filename, fileSize);
    }
}
