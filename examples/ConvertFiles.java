import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.http.Config;
import java.util.List;
import java.util.Map;

/**
 * Guide: Convert Files — discover the conversions catalog, then run a conversion.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" ConvertFiles
 * </pre>
 */
public final class ConvertFiles {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // The whole catalog of supported conversions (no auth, no quota).
        List<Map<String, Object>> all = client.conversions().list();
        System.out.println("Catalog size: " + all.size());

        // Narrow it to conversions that target PNG (our source here is a JPG).
        List<Map<String, Object>> toPng = client.conversions().list(null, "png", 1);
        System.out.println("Conversions to png: " + toPng.size());

        // Now perform the JPG -> PNG conversion.
        ConversionResult result = client.convert(REMOTE_JPG, "png");
        System.out.println("Saved: " + result.save("./"));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private ConvertFiles() {
    }
}
