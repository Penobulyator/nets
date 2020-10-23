import java.io.IOException;
import java.text.DecimalFormat;

public class View {
    private static final String[] WINDOWS_CLEAR_COMMAND = {"cmd", "/c", "cls"};
    private static final String LINUX_CLEAR_COMMAND = "clear";
    private ProcessBuilder processBuilder;

    private static final String head = "Address        Port      Current speed  Average speed  Mb received";
    private static final int addressColWidth = 15;
    private static final int portColWidth = 10;
    private static final int currentSpeedColWidth = 15;
    private static final int averageSpeedColWidth = 15;
    private static final int receivedDataColWidth = 15;

    public View(){
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder = new ProcessBuilder(WINDOWS_CLEAR_COMMAND).inheritIO();
        } else {
            processBuilder = new ProcessBuilder(LINUX_CLEAR_COMMAND).inheritIO();
        }
    }
    public void clear() {
        try {
            processBuilder.start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        };

        System.out.println(head);
    }

    public void printSpaces(int N){
        String str = "";
        for (int i =0; i < N; i++){
            str = str.concat(" ");
        }
        System.out.print(str);
    }

    public void print(String address, int port, int currentSpeed, int averageSpeed, int receivedData){
        System.out.print(address);
        printSpaces(addressColWidth - address.length());

        System.out.print(port);
        printSpaces(portColWidth - Integer.toString(port).length());

        String currentSpeedString = String.format("%.2f", currentSpeed / Math.pow(1024, 2)) + "Mb/s";
        String averageSpeedString = String.format("%.2f", averageSpeed / Math.pow(1024, 2)) + "Mb/s";
        String receivedDataString = String.format("%.2f", receivedData / Math.pow(1024, 2)) + "Mb";

        System.out.print(currentSpeedString);
        printSpaces(currentSpeedColWidth - currentSpeedString.length());

        System.out.print(averageSpeedString);
        printSpaces(averageSpeedColWidth - averageSpeedString.length());

        System.out.print(receivedDataString);
        System.out.println("");
    }
}
