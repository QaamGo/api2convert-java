package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.SignatureVerificationException;
import com.api2convert.webhook.WebhookEvent;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookVerifierTest {

    private static final String SECRET = "whsec_test";

    @Test
    void constructEventVerifiesValidSignature() {
        String payload = "{\"id\":\"job-1\",\"status\":{\"code\":\"completed\"}}";
        String signature = hmac(payload, SECRET);

        WebhookEvent event = Api2Convert.webhooks().constructEvent(payload, signature, SECRET);

        assertEquals("job-1", event.job().id());
        assertTrue(event.job().isCompleted());
    }

    @Test
    void rejectsTamperedPayload() {
        String payload = "{\"id\":\"job-1\",\"status\":{\"code\":\"completed\"}}";
        String signature = hmac(payload, SECRET);
        String tampered = payload + " ";

        assertThrows(SignatureVerificationException.class,
                () -> Api2Convert.webhooks().constructEvent(tampered, signature, SECRET));
    }

    @Test
    void rejectsMissingSignatureWhenSecretGiven() {
        assertThrows(SignatureVerificationException.class,
                () -> Api2Convert.webhooks().constructEvent("{}", null, SECRET));
    }

    @Test
    void parseSkipsVerificationWithEmptySecret() {
        String payload = "{\"id\":\"job-2\",\"status\":{\"code\":\"processing\"}}";

        WebhookEvent event = Api2Convert.webhooks().constructEvent(payload, null, "");

        assertEquals("job-2", event.job().id());
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows(SignatureVerificationException.class,
                () -> Api2Convert.webhooks().parse("not-json"));
    }

    @Test
    void rejectsValidJsonThatIsNotAnObject() {
        // `123` is valid JSON but decodes to a scalar, not a job object.
        assertThrows(SignatureVerificationException.class,
                () -> Api2Convert.webhooks().parse("123"));
    }

    private static String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
