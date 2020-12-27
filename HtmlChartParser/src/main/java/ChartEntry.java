import lombok.Getter;

public class ChartEntry {
    @Getter
    private String title;

    @Getter
    private String author;


    public ChartEntry(String title, String author) {
        this.title = title;
        this.author = author;
    }
}
