import com.api2convert.Api2Convert;
import com.api2convert.AsyncOptions;
import com.api2convert.exception.SignatureVerificationException;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import com.api2convert.webhook.WebhookEvent;

/**
 * Guide: Webhooks — start a conversion asynchronously and be notified via a callback.
 *
 * <p>{@link #main} kicks off an async job with a callback URL and returns immediately with a STARTED
 * job. {@link #handle} is the receiver side: verify the signed payload before trusting it.
 *
 * <p>Run (SDK + Jackson on the classpath):
 * <pre>
 *   API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Webhooks
 * </pre>
 */
public final class Webhooks {

    private static final String REMOTE_DOCX =
            "https://example-files.online-convert.com/document/docx/example.docx";
    private static final String CALLBACK_URL =
            "https://your-app.example.com/api2convert/webhook";

    public static void main(String[] args) {
        Api2Convert client = newClient();

        // Fire-and-forget: the API notifies CALLBACK_URL when the job's status changes.
        Job job = client.convertAsync(REMOTE_DOCX, "pdf", null,
                new AsyncOptions().category("document").callback(CALLBACK_URL));

        System.out.println("Started job " + job.id() + " (" + job.status().code() + ")");
        System.out.println("A webhook will POST to " + CALLBACK_URL + " when it finishes.");
    }

    /**
     * Receiver side. Point the job's {@code callback} at your endpoint, read the RAW request body and
     * the {@code X-Oc-Signature} header, and verify before trusting the payload.
     */
    public static void handle(byte[] rawBody, String signatureHeader) {
        // Fail closed: an empty secret makes constructEvent() skip verification entirely, so refuse
        // to run rather than trust an unverified body. If your account has not enabled signed
        // webhooks yet, switch deliberately to Api2Convert.webhooks().parse(rawBody) instead.
        String secret = System.getenv("API2CONVERT_WEBHOOK_SECRET");
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                    "API2CONVERT_WEBHOOK_SECRET is not set; refusing to accept unverified webhooks.");
        }

        WebhookEvent event;
        try {
            event = Api2Convert.webhooks().constructEvent(rawBody, signatureHeader, secret);
        } catch (SignatureVerificationException e) {
            // respond 400 Bad Request
            return;
        }

        var job = event.job();
        if (job.isCompleted()) {
            for (OutputFile output : job.output()) {
                // e.g. enqueue a download of output.uri()
                System.out.println("Job " + job.id() + " done: " + output.uri());
            }
        } else if (job.isFailed()) {
            String message = job.errors().isEmpty() ? "unknown" : job.errors().get(0).message();
            System.out.println("Job " + job.id() + " failed: " + message);
        }
        // respond 200 OK
    }

    private static Api2Convert newClient() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? new Api2Convert("", Config.builder().baseUrl(base).build())
                : new Api2Convert("");
    }

    private Webhooks() {
    }
}
