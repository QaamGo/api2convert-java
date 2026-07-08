import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import java.time.YearMonth;

/**
 * Guide: Statistics — read your API usage for a given month.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Statistics
 * </pre>
 */
public final class Statistics {

    public static void main(String[] args) {
        Api2Convert client = newClient();

        String month = YearMonth.now().toString(); // e.g. "2026-06"
        Object stats = client.stats().month(month);

        System.out.println("Stats for " + month + ": " + stats);
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private Statistics() {
    }
}
