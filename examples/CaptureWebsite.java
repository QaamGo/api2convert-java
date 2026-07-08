import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.util.List;
import java.util.Map;

/**
 * Guide: Capture a Website — screenshot a URL to an image via the screenshot engine.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" CaptureWebsite
 * </pre>
 */
public final class CaptureWebsite {

    public static void main(String[] args) {
        Api2Convert client = newClient();

        Job job = client.jobs().create(Map.of(
                "process", true,
                "input", List.of(Map.of(
                        "type", "remote",
                        "source", "https://www.online-convert.com",
                        "engine", "screenshot",
                        "options", Map.of(
                                "screen_width", 1280,
                                "screen_height", 1024,
                                "device_scale_factor", 1))),
                "conversion", List.of(Map.of("category", "image", "target", "png"))));

        Job done = client.jobs().await(job.id());
        System.out.println("Status: " + done.status().code());
        for (OutputFile output : done.output()) {
            System.out.println("Screenshot: " + output.uri());
        }
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private CaptureWebsite() {
    }
}
