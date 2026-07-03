import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.exception.Api2ConvertException;
import java.nio.file.Paths;

/**
 * Minimal end-to-end example.
 *
 * <p>Run (with the SDK + its dependency on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "path/to/classes:libs/*" Convert path/to/file.docx pdf
 * </pre>
 */
public final class Convert {

    public static void main(String[] args) {
        String input = args.length > 0
                ? args[0]
                : "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";
        String target = args.length > 1 ? args[1] : "png";

        Api2Convert client = new Api2Convert(""); // reads API2CONVERT_API_KEY

        try {
            ConversionResult result = client.convert(input, target);
            String path = result.save(Paths.get("").toAbsolutePath().toString() + "/");
            System.out.println("Saved: " + path);
        } catch (Api2ConvertException e) {
            System.err.println("Conversion failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private Convert() {
    }
}
