import lombok.Data;
import lombok.Getter;

@Data
public class ChartEntry {
    @Getter
    private final String title;

    @Getter
    private final String author;
}
