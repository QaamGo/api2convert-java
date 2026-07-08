import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.http.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guide: Uploading Files — one call uploads a LOCAL file and converts it.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" UploadingFiles
 * </pre>
 */
public final class UploadingFiles {

    /** A minimal valid 1x1 PNG, written to disk so the real multipart upload runs. */
    private static final byte[] ONE_PX_PNG = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
        (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
        0x00, 0x00, 0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
        0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82,
    };

    public static void main(String[] args) throws IOException {
        Api2Convert client = newClient();

        // A local file — convert() uploads it for you.
        Path src = Files.createTempDirectory("a2c-upload").resolve("pixel.png");
        Files.write(src, ONE_PX_PNG);

        ConversionResult result = client.convert(src.toString(), "png");
        System.out.println("Saved: " + result.save("./"));
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private UploadingFiles() {
    }
}
