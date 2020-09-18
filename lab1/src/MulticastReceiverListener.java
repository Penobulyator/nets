import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class MulticastReceiverListener extends Thread{
    private Map<InetAddress, Integer> connections = new HashMap<>();

    private static final String[] WINDOWS_CLEAR_COMMAND = {"cmd", "/c", "cls"};
    private static final String LINUX_CLEAR_COMMAND = "clear";
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static ProcessBuilder processBuilder;
    public void addConnection(InetAddress address){
        synchronized (connections){
            if (connections.containsKey(address))
                connections.replace(address, Protocol.maxSilenceTime);
            else
                connections.put(address, Protocol.maxSilenceTime);
        }
    }

    private void clear(){
        try {
            processBuilder.start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder = new ProcessBuilder(WINDOWS_CLEAR_COMMAND).inheritIO();
        } else {
            processBuilder = new ProcessBuilder(LINUX_CLEAR_COMMAND).inheritIO();
        }
        while(!currentThread().isInterrupted()){
            clear();
            System.out.println("Hosts alive:");
            synchronized (connections){
                for(Map.Entry<InetAddress, Integer> connection: connections.entrySet())
                    if (connection.getValue() != 0){
                        System.out.println(connection.getKey().getHostAddress() + ", last message received " + (Protocol.maxSilenceTime - connection.getValue()) + " seconds ago");
                        connection.setValue(connection.getValue() - 1);
                    }
            }
            try {
                sleep(Protocol.resendTime * 1000);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
