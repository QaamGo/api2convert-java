import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import java.nio.charset.StandardCharsets;

/**
 * Guide: Create Hashes — compute a checksum (SHA-256) of a file.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" CreateHashes
 * </pre>
 */
public final class CreateHashes {

    private static final String REMOTE_ZIP =
            "https://example-files.online-convert.com/archive/zip/example.zip";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_ZIP, "sha256", null,
                new ConvertOptions().category("hash"));

        // The output holds the computed digest — save it, and print it.
        System.out.println("Saved: " + result.save("./"));
        System.out.println("Digest: " + new String(result.contents(), StandardCharsets.UTF_8).trim());
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private CreateHashes() {
    }
}
