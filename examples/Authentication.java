import com.api2convert.Api2Convert;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import java.util.List;

/**
 * Guide: Authentication — verify your API key works by making an authenticated call.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Authentication
 * </pre>
 */
public final class Authentication {

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // A successful, authenticated call confirms the key is valid.
        List<Job> jobs = client.jobs().list();
        System.out.println("Authenticated. Your recent jobs: " + jobs.size());
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private Authentication() {
    }
}
