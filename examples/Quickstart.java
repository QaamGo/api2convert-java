import com.api2convert.Api2Convert;
import com.api2convert.ConversionResult;
import com.api2convert.http.Config;
import com.api2convert.model.Job;

/**
 * Guide: Quick Start — convert a remote file, look up the job, download the output.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Quickstart
 * </pre>
 */
public final class Quickstart {

    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // One call: create the job, fetch the remote input, wait, and hand back a result.
        ConversionResult result = client.convert(REMOTE_JPG, "png");

        // Look the job back up by id (the Jobs API).
        Job job = client.jobs().get(result.job().id());
        System.out.println("Job " + job.id() + " status: " + job.status().code());

        // Download the produced file to the current directory.
        String path = result.save("./");
        System.out.println("Saved: " + path);
    }

    /** Reads the key from API2CONVERT_API_KEY; honors API2CONVERT_BASE_URL when set. */
    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private Quickstart() {
    }
}
