import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class MulticastReceiverListener extends Thread{
    private Map<InetAddress, Integer> connections = new HashMap<>();

    private static final String[] CLEAR_COMMAND = {"cmd", "/c", "cls"};
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final ProcessBuilder processBuilder = new ProcessBuilder(CLEAR_COMMAND).inheritIO();

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
            if (OS.contains("windows")){
                processBuilder.start().waitFor();
            }
            else{
                Runtime.getRuntime().exec("clear");
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(!currentThread().isInterrupted()){
            clear();
            System.out.println("Hosts alive:");
            synchronized (connections){
                for(Map.Entry<InetAddress, Integer> connection: connections.entrySet())
                    if (connection.getValue() == 0){
                        connections.remove(connection.getKey());
                    }
                    else{
                        System.out.println(connection.getKey().getHostAddress() + " " + connection.getValue());
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
