package com.api2convert.webhook;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.exception.SignatureVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies and parses webhook callbacks.
 *
 * <p>Pass the <strong>raw</strong> request body (bytes, or the exact string received) so signature
 * verification is byte-exact. Verification uses HMAC-SHA256 and matches the server's signed-webhooks
 * scheme; until signed webhooks are enabled on your account no signature is sent — use {@link #parse}
 * then, or call {@link #constructEvent} with an empty secret to skip verification.
 */
public final class WebhookVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Verify the signature (when a secret is given) and return the typed event.
     *
     * @param payload   the raw request body
     * @param signature the signature header value (e.g. {@code X-Oc-Signature})
     * @param secret    your webhook signing secret; pass {@code ""} to skip verification
     * @throws SignatureVerificationException when the signature is missing or does not match
     */
    public WebhookEvent constructEvent(byte[] payload, String signature, String secret) {
        if (secret != null && !secret.isEmpty()) {
            if (signature == null || signature.isEmpty()) {
                throw new SignatureVerificationException("Missing webhook signature header.");
            }
            String expected = hmacSha256Hex(payload, secret);
            // Constant-time comparison so a wrong signature cannot be recovered by timing.
            if (!MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                throw new SignatureVerificationException("Webhook signature verification failed.");
            }
        }
        return parse(payload);
    }

    public WebhookEvent constructEvent(String payload, String signature, String secret) {
        return constructEvent(payload.getBytes(StandardCharsets.UTF_8), signature, secret);
    }

    /**
     * Parse a callback body into a typed event WITHOUT verifying a signature. Only use this when
     * signed webhooks are not yet enabled for your account.
     *
     * @throws SignatureVerificationException when the body is not a valid JSON object
     */
    @SuppressWarnings("unchecked")
    public WebhookEvent parse(byte[] payload) {
        Object decoded;
        try {
            decoded = MAPPER.readValue(payload, Object.class);
        } catch (IOException e) {
            throw new SignatureVerificationException("Webhook payload is not valid JSON: " + e.getMessage());
        }
        if (!(decoded instanceof Map)) {
            throw new SignatureVerificationException("Webhook payload is not a JSON object.");
        }
        return WebhookEvent.fromMap((Map<String, Object>) decoded);
    }

    public WebhookEvent parse(String payload) {
        return parse(payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256Hex(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload);
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new Api2ConvertException("Could not compute the webhook signature: " + e.getMessage(), e);
        }
    }
}
