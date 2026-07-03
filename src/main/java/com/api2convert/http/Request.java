package com.api2convert.http;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A transport-agnostic HTTP request. Either {@link #body()} (a byte array, replayable) or
 * {@link #streamBody()} (a one-shot stream, not replayable) is set — never both.
 *
 * @param method          HTTP method
 * @param uri             absolute request URI
 * @param headers         request headers (single-valued)
 * @param body            request body as bytes, or null
 * @param streamBody      a supplier of the request body stream (for multipart uploads), or null
 * @param followRedirects whether the sender may follow a redirect for this request. Must be false
 *                        for any request carrying an auth header (the account key / per-job token
 *                        travel in custom headers that a redirect-following client would forward to
 *                        another host); only the self-contained, no-auth download path sets it true.
 * @param replayable      whether the body can be re-sent from the start (a byte-array/empty body can;
 *                        a one-shot stream cannot, so it is sent exactly once)
 */
public record Request(
        String method,
        String uri,
        Map<String, String> headers,
        byte[] body,
        Supplier<InputStream> streamBody,
        boolean followRedirects,
        boolean replayable) {

    /** A request with a byte-array (or empty) body — replayable. */
    public static Request of(String method, String uri, Map<String, String> headers,
                             byte[] body, boolean followRedirects) {
        return new Request(method, uri, headers, body, null, followRedirects, true);
    }

    /** A request with a one-shot streamed body (multipart upload) — not replayable. */
    public static Request streaming(String method, String uri, Map<String, String> headers,
                                    Supplier<InputStream> streamBody) {
        return new Request(method, uri, headers, null, streamBody, false, false);
    }
}
