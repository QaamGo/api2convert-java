package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.ValidationException;
import com.api2convert.model.CloudInput;
import com.api2convert.model.OutputTarget;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Fixture 3 — credential redaction (the security test). The single secret {@code SUPERSECRET123}
 * must never surface and {@code [REDACTED]} must appear wherever a credentials object is rendered.
 * Runs in the SDK's security suite alongside {@link SecurityTest}.
 */
class CloudRedactionTest extends A2CTestCase {

    private static final String SECRET = "SUPERSECRET123";

    // --- 3a — object rendering ------------------------------------------------------------------

    @Test
    void cloudInputToStringMasksCredentials() {
        CloudInput input = CloudInput.amazonS3("b", "f", "AKIA", SECRET);

        String rendered = input.toString();

        assertFalse(rendered.contains(SECRET), "a credential value must never render");
        assertTrue(rendered.contains("[REDACTED]"), "the credentials object must render as [REDACTED]");
        // Non-secret parameters still render normally.
        assertTrue(rendered.contains("bucket=b"));
    }

    @Test
    void outputTargetToStringMasksCredentials() {
        OutputTarget target = OutputTarget.of("ftp",
                Map.of("host", "ftp.example.com"), Map.of("password", SECRET));

        String rendered = target.toString();

        assertFalse(rendered.contains(SECRET));
        assertTrue(rendered.contains("[REDACTED]"));
        assertTrue(rendered.contains("host=ftp.example.com"));
    }

    // --- 3b — error text on the create path -----------------------------------------------------

    @Test
    void secretDoesNotLeakIntoACreatePathError() {
        // A create carrying cloud credentials fails validation; neither the message nor the attached
        // body may contain the submitted secret.
        http.addJson(422, "{\"message\":\"Validation failed\"}");

        CloudInput input = CloudInput.amazonS3("b", "f", "AKIA", SECRET);
        Map<String, Object> payload = Map.of(
                "process", true,
                "input", List.of(input.toDescriptor()),
                "conversion", List.of(Map.of("target", "jpg")));

        ValidationException e = assertThrows(ValidationException.class,
                () -> client().jobs().create(payload));

        assertFalse(e.getMessage().contains(SECRET), "the secret must not appear in the exception message");
        assertFalse(e.getBody().toString().contains(SECRET), "the secret must not appear in the attached body");
    }

    // --- 3c — error-body deep-walk (belt-and-suspenders) ----------------------------------------

    @Test
    void echoedSecretInNestedErrorBodyIsRedacted() {
        http.addJson(422, """
                { "message": "Validation failed",
                  "errors": { "input.0.credentials.secretaccesskey": "SUPERSECRET123" } }
                """);

        ValidationException e = assertThrows(ValidationException.class,
                () -> client().jobs().create(Map.of("conversion", List.of(Map.of("target", "jpg")))));

        String body = e.getBody().toString();
        assertFalse(body.contains(SECRET), "an echoed credential value must be scrubbed from the decoded body");
        assertTrue(body.contains("[REDACTED]"), "the scrubbed leaf must render as [REDACTED]");
    }

    // --- 3d — sensitive parameters leaf ---------------------------------------------------------

    @Test
    void sensitiveParametersLeafIsMaskedWhileNonSecretKeysRenderNormally() {
        CloudInput input = CloudInput.of("amazons3",
                orderedParams(), Map.of());

        String rendered = input.toString();

        assertFalse(rendered.contains("PARAMSECRET"), "a sensitive parameters leaf must be masked");
        assertTrue(rendered.contains("[REDACTED]"));
        // Non-secret parameter keys render normally.
        assertTrue(rendered.contains("bucket=my-bucket"));
        assertTrue(rendered.contains("host=ftp.example.com"));
    }

    private static Map<String, Object> orderedParams() {
        java.util.Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("token", "PARAMSECRET");
        params.put("bucket", "my-bucket");
        params.put("host", "ftp.example.com");
        return params;
    }
}
