import jdk.nashorn.internal.objects.annotations.Getter;

public class ChartEntry {
    private String title;

    private String author;

    public ChartEntry(String title, String author) {
        this.title = title;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }
}
