import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import java.nio.charset.StandardCharsets;

/**
 * Guide: File Analysis — extract a file's metadata as JSON.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" FileAnalysis
 * </pre>
 */
public final class FileAnalysis {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_JPG, "json", null,
                new ConvertOptions().category("metadata"));

        System.out.println("Metadata JSON:");
        System.out.println(new String(result.contents(), StandardCharsets.UTF_8));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private FileAnalysis() {
    }
}
