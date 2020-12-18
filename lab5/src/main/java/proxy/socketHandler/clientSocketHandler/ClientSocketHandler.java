package proxy.socketHandler.clientSocketHandler;

import proxy.socketHandler.messageForwarder.MessageForwarder;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class ClientSocketHandler {
    SocketChannel readSocketChannel;
    SocketChannel writeSocketChanel = null;

    MessageForwarder forwarder;

    ClientSocketHandlerListener listener;

    State state;




    public ClientSocketHandler(SocketChannel readSocketChannel, ClientSocketHandlerListener listener) {
        this.readSocketChannel = readSocketChannel;
        this.listener = listener;
        state = State.READING_GREETING;
    }

    private void readGreeting(){
        /*
       +----+----------+----------+
       |VER | NMETHODS | METHODS  |
       +----+----------+----------+
       | 1  |    1     | 1 to 255 |
       +----+----------+----------+
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(257);
        int length = 0;
        try {
            length = readSocketChannel.read(byteBuffer);
        } catch (IOException e) {
            listener.closeSocket(readSocketChannel);
        }
        if (length == 0){
            return;
        }

        byte[] bufBytes = Arrays.copyOfRange(byteBuffer.array(), 0, length);

        //System.out.println("Reading greeting: " + Arrays.toString(bufBytes));

        if (bufBytes[0] != 0x05){
            System.out.println("Client does't support SOCKS5");
        }

        if (bufBytes[1] != 0x01 || bufBytes[2] != 0x00){
            System.out.println("Authentication required");
        }

        state = State.SENDING_CHOICE;
    }

    private void sendChoice() throws IOException {
        /*
         +----+--------+
         |VER | METHOD |
         +----+--------+
         | 1  |   1    |
         +----+--------+
         */


        byte []message =  {0x05, 0x00};

        //System.out.println("Sending server choice: " + Arrays.toString(message));

        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length);
        byteBuffer.put(message);
        byteBuffer.flip();
        readSocketChannel.write(byteBuffer);

        state = State.READING_CONNECTION_REQUEST;
    }

    private void readConnectionRequest() throws IOException {
        /*
        +----+-----+-------+------+----------+----------+
        |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+
         */


        ByteBuffer byteBuffer = ByteBuffer.allocate(300);
        int length;
        try {
            length = readSocketChannel.read(byteBuffer);
        } catch (IOException e) {
            listener.closeSocket(readSocketChannel);
            return;
        }
        if (length == 0){
            return;
        }

        byte [] bufBytes = Arrays.copyOfRange( byteBuffer.array(), 0, length);

        //System.out.println("Reading connection request: " + Arrays.toString(bufBytes));

        if (bufBytes[0] != 0x05){
            System.out.println("Client does't support SOCKS5");
        }

        if (bufBytes[1] != 0x01){
            System.out.println("Command is not a CONNECT");
        }

        int addressLength = bufBytes[4];
        if (bufBytes[3] == 0x04){
            System.out.println("Client wants to establish IPv6 connection");
        }

        String address = new String(Arrays.copyOfRange(bufBytes, 5, 5 + addressLength));

        byte[] portBytes =  {bufBytes[length -2], bufBytes[length - 1]};
        int port = ByteBuffer.wrap(portBytes).getShort();

        InetSocketAddress inetSocketAddress = new InetSocketAddress(address, port);
        writeSocketChanel = SocketChannel.open();

        writeSocketChanel.connect(inetSocketAddress);

        listener.addServerSocketListener(writeSocketChanel, readSocketChannel);

        forwarder = new MessageForwarder(readSocketChannel, writeSocketChanel);

        state = State.SENDING_RESPONSE;
    }

    private void sendResponse() throws IOException {
        /*
        +----+-----+-------+------+----------+----------+
        |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
        +----+-----+-------+------+----------+----------+
        | 1  |  1  | X'00' |  1   | Variable |    2     |
        +----+-----+-------+------+----------+----------+
         */


        byte []message = {0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        //System.out.println("Sending connection response: " + Arrays.toString(message));

        ByteBuffer byteBuffer = ByteBuffer.allocate(message.length);
        byteBuffer.put(message);
        byteBuffer.flip();

        readSocketChannel.write(byteBuffer);

        state = State.FORWARDING;
    }

    public void handle(SelectionKey selectionKey) throws IOException {
        if (!selectionKey.isValid())
            return;

        switch (state){

            case READING_GREETING:
                if (selectionKey.isReadable()){
                    readGreeting();
                }
                break;
            case SENDING_CHOICE:
                if (selectionKey.isWritable()){
                    sendChoice();
                }
                break;
            case READING_CONNECTION_REQUEST:
                if (selectionKey.isReadable()){
                    readConnectionRequest();
                }
                break;
            case SENDING_RESPONSE:
                if (selectionKey.isWritable()){
                    sendResponse();
                }
                break;
            case FORWARDING:
                if (selectionKey.channel().equals(readSocketChannel) && selectionKey.isReadable()){
                    boolean success = forwarder.read();
                    if (!success){
                        listener.closeSocket(readSocketChannel);
                        return;
                    }
                }
                else if (selectionKey.channel().equals(writeSocketChanel) && selectionKey.isWritable()){
                    boolean success = forwarder.write();
                    if (!success){
                        listener.closeSocket(writeSocketChanel);
                        return;
                    }
                }
                break;
        }
    }

}
