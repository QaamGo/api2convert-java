import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.util.List;
import java.util.Map;

/**
 * Guide: The Job Lifecycle — drive create -&gt; add input -&gt; start -&gt; wait -&gt; outputs by hand.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" JobLifecycle
 * </pre>
 */
public final class JobLifecycle {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // Stage a job (process=false) so we can attach inputs before starting.
        Job job = client.jobs().create(Map.of(
                "process", false,
                "conversion", List.of(Map.of("category", "image", "target", "png"))));
        System.out.println("Created job " + job.id());

        // Attach a remote input, then start processing.
        client.jobs().addInput(job.id(), Map.of("type", "remote", "source", REMOTE_JPG));
        client.jobs().start(job.id());

        // Poll to a terminal status.
        Job done = client.jobs().await(job.id());
        System.out.println("Status: " + done.status().code());

        for (OutputFile output : done.output()) {
            System.out.println("Output: " + output.uri());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private JobLifecycle() {
    }
}
