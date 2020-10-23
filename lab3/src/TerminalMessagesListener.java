import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TerminalMessagesListener implements Runnable{

    public TerminalMessagesListener(ChatNode chatNode) {
        this.chatNode = chatNode;
    }

    ChatNode chatNode;

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (!Thread.currentThread().isInterrupted()){
            try {
                String line = reader.readLine();
                chatNode.sendMessage(line);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
        }
    }
}
