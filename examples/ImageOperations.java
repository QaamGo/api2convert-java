import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import java.util.Map;

/**
 * Guide: Image Operations — resize an image with the "resize-image" operation.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" ImageOperations
 * </pre>
 */
public final class ImageOperations {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_JPG, "resize-image",
                Map.of(
                        "width", 800,
                        "height", 600,
                        "resize_by", "px",
                        "resize_handling", "keep_aspect_ratio_crop"),
                new ConvertOptions().category("operation"));

        System.out.println("Saved: " + result.save("./"));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private ImageOperations() {
    }
}
