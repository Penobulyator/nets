import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class PingSender implements Runnable{
    private final static int PERIOD = 1000;
    MessageSender messageSender;
    Set<Connection> connectionSet;

    public PingSender(MessageSender messageSender, Set<Connection> connectionSet) {
        this.messageSender = messageSender;
        this.connectionSet = connectionSet;
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            for (Connection connection: connectionSet) {
                //send ping to all connections
                Message ping = new Message(Protocol.Flags.PING, UUID.randomUUID());
                try {
                    messageSender.sendMessage(ping, connection);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    return;
                }
            }

            try {
                Thread.sleep(PERIOD);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
