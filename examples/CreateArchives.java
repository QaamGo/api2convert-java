import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.util.List;
import java.util.Map;

/**
 * Guide: Create Archives — bundle several files into a single ZIP.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" CreateArchives
 * </pre>
 */
public final class CreateArchives {

    private static final String REMOTE_PDF =
            "https://example-files.online-convert.com/document/pdf/example.pdf";
    private static final String REMOTE_PNG =
            "https://example-files.online-convert.com/raster%20image/png/example.png";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        Job job = client.jobs().create(Map.of(
                "process", true,
                "input", List.of(
                        Map.of("type", "remote", "source", REMOTE_PDF),
                        Map.of("type", "remote", "source", REMOTE_PNG)),
                "conversion", List.of(Map.of("category", "archive", "target", "zip"))));

        Job done = client.jobs().await(job.id());
        System.out.println("Status: " + done.status().code());
        for (OutputFile output : done.output()) {
            System.out.println("Archive: " + output.uri());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private CreateArchives() {
    }
}
