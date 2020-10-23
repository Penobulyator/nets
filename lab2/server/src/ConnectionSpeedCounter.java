import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ConnectionSpeedCounter extends Thread{
    private static final View view = new View();
    private static final int REFRESH_PERIOD = 1;

    //se need to store: <receiver> < <time connected>, <received bytes> >
    private final Map<FileReceiver, AbstractMap.SimpleEntry<Integer, Long>> receiversTimeAlive = new ConcurrentHashMap<>();

    public void addReceiver(FileReceiver receiver){
        synchronized (receiversTimeAlive){
            receiversTimeAlive.put(receiver, new  AbstractMap.SimpleEntry<>(0, 0L));
        }
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()){
            view.clear();
            synchronized (receiversTimeAlive){
                for (Map.Entry<FileReceiver,  AbstractMap.SimpleEntry<Integer, Long>> entry: receiversTimeAlive.entrySet()){
                    FileReceiver receiver = entry.getKey();
                    int timeAlive = entry.getValue().getKey();
                    long receivedBytes = entry.getValue().getValue();

                    if (timeAlive > 0){
                        long totalBytesReceived = receiver.getReceivedBytesCount();
                        long currentSpeed = (totalBytesReceived - receivedBytes) / REFRESH_PERIOD;
                        long averageSpeed = totalBytesReceived / timeAlive; //in bytes/sec
                        view.print(receiver.getClientAddress(), receiver.getClientPort(), (int)currentSpeed, (int)averageSpeed, (int)totalBytesReceived);
                        if (!receiver.isAlive()){
                            receiversTimeAlive.remove(receiver);
                        }

                        receiversTimeAlive.replace(receiver, new  AbstractMap.SimpleEntry<>(timeAlive + REFRESH_PERIOD, totalBytesReceived));
                    }
                    else{
                        receiversTimeAlive.replace(receiver, new AbstractMap.SimpleEntry<>(timeAlive + REFRESH_PERIOD, receivedBytes));
                    }

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
