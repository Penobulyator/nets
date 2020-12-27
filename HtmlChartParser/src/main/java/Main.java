import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        List<ChartEntry> chartEntryList = HtmlChartParser.getChart("https://music.yandex.ru/chart/tracks");

        int counter = 1;
        for (ChartEntry chartEntry: chartEntryList){
            System.out.println(counter + ") " + chartEntry.getAuthor() + " - " + chartEntry.getTitle());
            counter++;
        }
    }
}
