package com.api2convert.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Credential redaction — cloud {@code credentials} travel in the request body, so they must never
 * surface in object inspection, a request log or a decoded error body.
 *
 * <p>Two masks:
 * <ul>
 *   <li>the <strong>whole</strong> {@code credentials} object is rendered as the fixed
 *       {@link #REDACTED} marker (see {@code CloudInput}/{@code OutputTarget} {@code toString});</li>
 *   <li>any {@code parameters} (or error-body) leaf whose key contains a sensitive token
 *       (case-insensitive substring) is replaced with the marker via {@link #maskSensitive}.</li>
 * </ul>
 *
 * <p>The API never echoes a credential <em>value</em> (only field names), so the error-body deep-walk
 * is belt-and-suspenders against a future server/proxy change. Internal helper, not public API.
 */
public final class Redaction {

    /** The fleet-wide redaction marker (D9). */
    public static final String REDACTED = "[REDACTED]";

    /**
     * Substrings that mark a key as sensitive (case-insensitive). A {@code parameters} leaf or an
     * error-body key containing any of these has its value masked.
     */
    private static final List<String> SENSITIVE = List.of(
            "token", "password", "passwd", "secret", "key", "keyfile",
            "credential", "passphrase", "sas", "sig", "signature");

    private Redaction() {
    }

    /** Whether {@code key} names a sensitive value (case-insensitive substring match). */
    public static boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        for (String needle : SENSITIVE) {
            if (lower.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a deep copy of {@code value} with every sensitive-keyed leaf replaced by {@link #REDACTED}.
     * Recurses through maps and lists; a sensitive key masks its whole value (even a nested object).
     * Non-map/list values pass through unchanged. Used both for {@code parameters} rendering and the
     * decoded error-body deep-walk (dotted/nested echoed keys such as
     * {@code input.0.credentials.secretaccesskey}).
     */
    public static Object maskSensitive(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                out.put(key, isSensitiveKey(key) ? REDACTED : maskSensitive(entry.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(maskSensitive(item));
            }
            return out;
        }
        return value;
    }
}
