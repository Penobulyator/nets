package proxy.socketHandler.messageForwarder;

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


    public boolean hasMessageToForward(){
        return !messageQueue.isEmpty();
    }

    //
    // Returns true if operation was successful
    //
    public boolean tryRead() {
        //System.out.println("Reading from " + readSocketChanel.getRemoteAddress().toString());
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 << 20);
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
    public boolean tryWrite() {
        if (!messageQueue.isEmpty()){
            int length = 0;
            try {
                length = writeSocketChanel.write(messageQueue.pop());
                //System.out.println("Forwarding from " + readSocketChanel.getRemoteAddress() + " to " + writeSocketChanel.getRemoteAddress() +" " + length + " bytes");

            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
