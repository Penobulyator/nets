package socketHandler.messageForwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

public class MessageForwarder {

    SocketChannel readSocketChanel;
    SocketChannel writeSocketChanel;

    ArrayDeque<ByteBuffer> messageQueue = new ArrayDeque<>();

    public MessageForwarder(SocketChannel readSocketChanel, SocketChannel writeSocketChanel) {
        this.readSocketChanel = readSocketChanel;
        this.writeSocketChanel = writeSocketChanel;
    }


    //
    // Returns true if operation was successful
    //
    public boolean read() throws IOException {
        //System.out.println("Reading from " + readSocketChanel.getRemoteAddress().toString());
        ByteBuffer byteBuffer = ByteBuffer.allocate(4046);
        try {
            readSocketChanel.read(byteBuffer);
        } catch (IOException e) {
            return false;
        }

        byteBuffer.flip();
        messageQueue.push(byteBuffer);
        return true;
    }

    //
    // Returns true if operation was successful
    //
    public boolean write() {
        if (!messageQueue.isEmpty()){
            try {
                writeSocketChanel.write(messageQueue.pop());
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
