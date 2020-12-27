import jdk.tools.jaotc.collect.jar.JarSourceProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class HtmlChartParser {
    public static List<ChartEntry> getChart(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .get();

        List<ChartEntry> chartEntryList = new LinkedList<>();
        Elements elements = doc.select("div.d-track__overflowable-wrapper.deco-typo-secondary");
        for (Element element: elements){
            String title = element.select("div.d-track__name").first().text();
            String author = element.select("span.d-track__artists").first().text();

            chartEntryList.add(new ChartEntry(title, author));
        }
        return chartEntryList;
    }
}
