import com.api2convert.Api2Convert;
import com.api2convert.exception.SignatureVerificationException;
import com.api2convert.model.OutputFile;
import com.api2convert.webhook.WebhookEvent;

/**
 * Example webhook receiver logic. Point a job's {@code callback} at your endpoint, read the RAW
 * request body and the {@code X-Oc-Signature} header, and verify before trusting the payload.
 *
 * <p>This is framework-agnostic pseudocode for the verify step; wire {@code rawBody} / {@code
 * signatureHeader} to your web framework's request.
 */
public final class Webhook {

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

    private Webhook() {
    }
}
