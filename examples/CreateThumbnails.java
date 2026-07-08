import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import java.util.Map;

/**
 * Guide: Create Thumbnails — render a preview image of the first page of a PDF.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" CreateThumbnails
 * </pre>
 */
public final class CreateThumbnails {

    private static final String REMOTE_PDF =
            "https://example-files.online-convert.com/document/pdf/example.pdf";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_PDF, "thumbnail",
                Map.of("thumbnail_target", "png", "width", 300, "pages", "first", "dpi", 150),
                new ConvertOptions().category("operation"));

        System.out.println("Saved: " + result.save("./"));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private CreateThumbnails() {
    }
}
