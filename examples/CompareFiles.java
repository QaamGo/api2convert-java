import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.util.List;
import java.util.Map;

/**
 * Guide: Compare Files — visually diff two images (SSIM).
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" CompareFiles
 * </pre>
 */
public final class CompareFiles {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";
    private static final String REMOTE_JPG_SMALL =
            "https://example-files.online-convert.com/raster%20image/jpg/example_small.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        Job job = client.jobs().create(Map.of(
                "process", true,
                "input", List.of(
                        Map.of("type", "remote", "source", REMOTE_JPG_SMALL),
                        Map.of("type", "remote", "source", REMOTE_JPG)),
                "conversion", List.of(Map.of(
                        "category", "operation",
                        "target", "compare-image",
                        "options", Map.of("method", "ssim", "threshold", 5, "diff_color", "red")))));

        Job done = client.jobs().await(job.id());
        System.out.println("Status: " + done.status().code());
        for (OutputFile output : done.output()) {
            System.out.println("Diff: " + output.uri());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private CompareFiles() {
    }
}
