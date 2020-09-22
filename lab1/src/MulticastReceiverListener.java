import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class MulticastReceiverListener extends Thread {
    private static final String[] WINDOWS_CLEAR_COMMAND = {"cmd", "/c", "cls"};
    private static final String LINUX_CLEAR_COMMAND = "clear";
    private final Map<Connection, Integer> connectionsSilenceTime = new TreeMap<>();
    private ProcessBuilder processBuilder;
    private static final int REFRESH_PERIOD = 1; //sec
    public void addConnection(Connection newConnection) {
        synchronized (connectionsSilenceTime) {
            if (connectionsSilenceTime.containsKey(newConnection)) {
                connectionsSilenceTime.replace(newConnection, 0);
            } else {
                connectionsSilenceTime.put(newConnection, 0);
            }
        }
    }

    private void clear() {
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

        while (!currentThread().isInterrupted()) {
            clear();
            System.out.println("Hosts alive:");
            synchronized (connectionsSilenceTime) {
                for (Map.Entry<Connection, Integer> connectionSilenceTime : connectionsSilenceTime.entrySet())
                    //print all the connection and increase silence time
                    if (connectionSilenceTime.getValue() != Protocol.maxSilenceTime) {
                        Connection connection = connectionSilenceTime.getKey();
                        System.out.println(connection.getIpAddress().getHostAddress() + ":" + connection.getPort() + ", last message received " + connectionSilenceTime.getValue() + " seconds ago");
                        connectionSilenceTime.setValue(connectionSilenceTime.getValue() + 1);
                    }
            }
            try {
                TimeUnit.SECONDS.sleep(REFRESH_PERIOD);
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }
    }
}
