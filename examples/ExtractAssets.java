import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.ConvertOptions;
import com.api2convert.http.Config;
import com.api2convert.model.OutputFile;

/**
 * Guide: Extract Assets — pull embedded assets (images, fonts, ...) out of a document.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" ExtractAssets
 * </pre>
 */
public final class ExtractAssets {

    private static final String REMOTE_DOCX =
            "https://example-files.online-convert.com/document/docx/example.docx";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        ConversionResult result = client.convert(REMOTE_DOCX, "extract-assets", null,
                new ConvertOptions().category("operation"));

        System.out.println("Outputs: " + result.outputs().size());
        for (OutputFile output : result.outputs()) {
            System.out.println("Asset: " + output.uri());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private ExtractAssets() {
    }
}
