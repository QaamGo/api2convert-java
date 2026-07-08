import com.api2convert.Api2Convert;
import com.api2convert.http.Config;

/**
 * Guide: Rate Limits &amp; Contracts — inspect your account's active contracts/quota.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" RateLimits
 * </pre>
 */
public final class RateLimits {

    public static void main(String[] args) {
        Api2Convert client = newClient();

        Object contracts = client.contracts().get();
        System.out.println("Contracts: " + contracts);
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private RateLimits() {
    }
}
