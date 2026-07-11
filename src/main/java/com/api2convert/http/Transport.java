package com.api2convert.http;

import com.api2convert.Api2Convert;
import com.api2convert.exception.ApiException;
import com.api2convert.exception.AuthenticationException;
import com.api2convert.exception.NetworkException;
import com.api2convert.exception.NotFoundException;
import com.api2convert.exception.PaymentRequiredException;
import com.api2convert.exception.RateLimitException;
import com.api2convert.exception.ServerException;
import com.api2convert.exception.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The HTTP layer: builds authenticated requests, retries transient failures with jittered
 * exponential backoff, maps error responses to typed exceptions and decodes JSON.
 *
 * <p>Resources talk to the API through {@link #request}; the file uploader and the downloader use
 * {@link #send} / {@link #interpret} / {@link #ensureSuccessful} directly because they need
 * non-JSON bodies and per-job auth.
 */
public final class Transport {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 500, 502, 503, 504);
    private static final Set<String> IDEMPOTENT_METHODS =
            Set.of("GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE");
    private static final double MAX_BACKOFF_SECONDS = 8.0;

    /**
     * Upper bound on how much of a control-plane (API / error) response body the SDK buffers into
     * memory, so a hostile or buggy server cannot force an unbounded read (OOM) on the JSON / error
     * path. File downloads are streamed to disk and bounded separately by {@code FileDownload}, so
     * this cap does not apply to them.
     */
    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024; // 16 MiB

    /**
     * Upper bound for an honored {@code Retry-After}. A server (or misconfigured proxy) asking for
     * an absurd delay can't stall a worker for hours — we never sleep longer than this per retry.
     */
    private static final double MAX_RETRY_AFTER_SECONDS = 120.0;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USER_AGENT =
            "api2convert-java/" + Api2Convert.VERSION + " java/" + System.getProperty("java.version");

    private final HttpSender http;
    private final Config config;
    private final String apiKey;
    private final Sleeper sleeper;

    public Transport(HttpSender http, Config config, String apiKey, Sleeper sleeper) {
        this.http = http;
        this.config = config;
        this.apiKey = apiKey;
        this.sleeper = sleeper;
    }

    public Config config() {
        return config;
    }

    /**
     * Sleep for (at least) the given seconds using the configured sleeper. Used by job polling; a
     * small upward jitter is added so a fleet that starts waiting at the same instant does not poll
     * in lockstep (thundering herd).
     */
    public void pause(double seconds) {
        sleeper.sleep(jitter(seconds));
    }

    public Object request(String method, String path) {
        return request(method, path, null, Map.of(), Map.of());
    }

    public Object request(String method, String path, Map<String, Object> body) {
        return request(method, path, body, Map.of(), Map.of());
    }

    public Object request(String method, String path, Map<String, Object> body, Map<String, String> query) {
        return request(method, path, body, query, Map.of());
    }

    /**
     * Perform an authenticated JSON request and return the decoded body (a {@code Map}, a
     * {@code List}, or an empty {@code Map}).
     */
    public Object request(String method, String path, Map<String, Object> body,
                          Map<String, String> query, Map<String, String> headers) {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("X-Oc-Api-Key", apiKey);
        requestHeaders.putAll(headers);

        byte[] bodyBytes = null;
        if (body != null) {
            bodyBytes = encode(body);
            requestHeaders.put("Content-Type", "application/json");
        }

        Request request = Request.of(method, url(path, query), requestHeaders, bodyBytes, false);
        return interpret(send(request));
    }

    /**
     * Send a fully-built request with retry/backoff. Adds the common Accept and User-Agent headers
     * but no auth — callers add the header they need.
     */
    public Response send(Request request) {
        Map<String, String> headers = new LinkedHashMap<>(request.headers());
        headers.putIfAbsent("Accept", "application/json");
        headers.putIfAbsent("User-Agent", USER_AGENT);
        Request effective = new Request(request.method(), request.uri(), headers, request.body(),
                request.streamBody(), request.followRedirects(), request.replayable());

        // A request may be retried only if its body can be replayed from the start. A one-shot
        // stream (a socket/pipe wrapped in a multipart upload) would re-send from an exhausted
        // position, producing a truncated/corrupt request — so it is sent exactly once.
        boolean replayable = effective.replayable();

        // A non-idempotent request (POST /jobs, /jobs/{id}/input, /presets, uploads) must not be
        // auto-retried on a 5xx or network error: the backend may have already acted on the first
        // attempt, so a blind retry would create a duplicate job — and a duplicate charge. Such
        // requests are retried only when they carry an Idempotency-Key. A 429 is safe to retry for
        // any method, since it is rejected before the request is processed.
        boolean idempotent = isIdempotent(effective);

        int attempt = 0;
        while (true) {
            Response response;
            try {
                response = http.send(effective);
            } catch (IOException e) {
                if (replayable && idempotent && attempt < config.maxRetries()) {
                    backoff(attempt, "");
                    attempt++;
                    continue;
                }
                throw new NetworkException("Request to API2Convert failed: " + e.getMessage(), e);
            }

            int status = response.status();
            boolean mayRetry = RETRYABLE_STATUSES.contains(status)
                    && replayable
                    && attempt < config.maxRetries()
                    && (status == 429 || idempotent);
            if (mayRetry) {
                String retryAfter = response.header("Retry-After");
                closeQuietly(response.body());
                backoff(attempt, retryAfter);
                attempt++;
                continue;
            }

            return response;
        }
    }

    /**
     * Throw a typed exception for error responses; otherwise decode the JSON body.
     */
    public Object interpret(Response response) {
        ensureSuccessful(response);

        // Every API request rides the no-follow path (secrets travel in X-Oc-* headers), so a 3xx
        // passes ensureSuccessful (status < 400) but was deliberately not followed; decoding its body
        // would yield an empty model. Surface it as a typed error instead (mirrors the download guard).
        int status = response.status();
        if (status >= 300 && status < 400) {
            throw new NetworkException(
                    "API2Convert returned an unexpected redirect (HTTP " + status + "); the request was not followed.");
        }

        byte[] raw = readBody(response);
        if (raw.length == 0) {
            return Map.of();
        }

        Object decoded;
        try {
            decoded = MAPPER.readValue(raw, Object.class);
        } catch (IOException e) {
            // A 2xx carrying a non-JSON body (an intermediary HTML/error page slipping through) must
            // still surface as an SDK exception, not a bare parse error that escapes the documented
            // Api2ConvertException hierarchy.
            throw new NetworkException("API2Convert returned a non-JSON success response: " + e.getMessage(), e);
        }

        if (decoded instanceof Map || decoded instanceof List) {
            return decoded;
        }
        return Map.of();
    }

    /**
     * Throw the appropriate typed exception when {@code response} is an HTTP error. On success the
     * body is left untouched (so a download can stream it).
     */
    public void ensureSuccessful(Response response) {
        int status = response.status();
        if (status < 400) {
            return;
        }

        Map<String, Object> body = decodeSafe(readBody(response));
        String apiMessage = body.get("message") instanceof String s ? s : null;
        String message = apiMessage != null ? apiMessage : "Request failed (HTTP " + status + ")";
        String requestId = emptyToNull(response.header("X-Request-Id"));

        if (status == 401 || status == 403) {
            throw new AuthenticationException(message, status, requestId, body);
        }
        if (status == 402) {
            throw new PaymentRequiredException(message, status, requestId, body);
        }
        if (status == 404) {
            throw new NotFoundException(message, status, requestId, body);
        }
        if (status == 429) {
            throw new RateLimitException(message, status, requestId, body,
                    parseRetryAfter(response.header("Retry-After")));
        }
        if (status == 400 || status == 422) {
            throw new ValidationException(message, status, requestId, body);
        }
        if (status >= 500) {
            throw new ServerException(message, status, requestId, body);
        }
        throw new ApiException(message, status, requestId, body);
    }

    /**
     * Download from a (self-contained) URL and return the body stream. Used for output downloads.
     *
     * <p>Redirect-following is enabled ONLY when the request carries no secret. The account key/token
     * never use this path, but a download password travels in the custom {@code X-Oc-Download-Password}
     * header, and the JDK client forwards custom headers across a cross-host redirect — so a request
     * carrying any {@code X-Oc-*} header must not follow redirects (upholding the "a request carrying a
     * secret never follows a redirect" guarantee). A plain, passwordless download URL may still redirect
     * (storage/CDN), and there is no secret on it to leak.
     */
    public InputStream download(String uri, Map<String, String> headers) {
        boolean carriesSecret = headers.keySet().stream()
                .anyMatch(name -> name.regionMatches(true, 0, "X-Oc-", 0, 5));
        Request request = Request.of("GET", uri, new LinkedHashMap<>(headers), null, !carriesSecret);
        Response response = send(request);
        ensureSuccessful(response);

        // ensureSuccessful() only rejects status >= 400, so a 3xx slips through. It can only reach
        // here on the no-follow path (a request carrying a secret): the redirect was deliberately not
        // followed, so the body is the storage redirect page — not the file. Writing it to disk would
        // silently corrupt the download; surface it as a network error instead.
        int status = response.status();
        if (status >= 300 && status < 400) {
            closeQuietly(response.body());
            throw new NetworkException("The download did not resolve: a redirect was not followed "
                    + "because the request carried a secret header.");
        }
        return response.body();
    }

    /**
     * Percent-encode a single dynamic path segment. IDs, dates and filters are interpolated into the
     * request path by the resources; a value containing a {@code "/"}, {@code "?"} or {@code "#"}
     * would otherwise alter the path structure — traversing to another resource or starting the
     * query/fragment. The fixed {@code "/"} separators between segments are added by the caller and
     * are never routed through here. Uses {@code %20} for spaces (a path segment, not a form field).
     */
    public static String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public String url(String path, Map<String, String> query) {
        String url = config.baseUrl() + "/" + (path.startsWith("/") ? path.substring(1) : path);
        if (!query.isEmpty()) {
            StringBuilder sb = new StringBuilder(url).append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
            url = sb.toString();
        }
        return url;
    }

    private byte[] encode(Map<String, Object> body) {
        try {
            return MAPPER.writeValueAsBytes(body);
        } catch (IOException e) {
            throw new NetworkException("Could not encode request body as JSON: " + e.getMessage(), e);
        }
    }

    private byte[] readBody(Response response) {
        try (InputStream in = response.body()) {
            // Read at most one byte past the cap: a body streamed by the JDK client (BodyHandlers
            // .ofInputStream) is pulled lazily, so a bounded read never buffers the whole of a
            // hostile/oversized body. A returned length beyond the cap means the body exceeds it.
            byte[] raw = in.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (raw.length > MAX_RESPONSE_BYTES) {
                throw new NetworkException("API response body exceeds 16 MiB");
            }
            return raw;
        } catch (IOException e) {
            throw new NetworkException("Could not read the API response body: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeSafe(byte[] raw) {
        if (raw.length == 0) {
            return Map.of();
        }
        try {
            Object decoded = MAPPER.readValue(raw, Object.class);
            return decoded instanceof Map ? (Map<String, Object>) decoded : Map.of();
        } catch (IOException e) {
            return Map.of();
        }
    }

    private void backoff(int attempt, String retryAfterHeader) {
        Integer retry = parseRetryAfter(retryAfterHeader);
        double seconds;
        if (retry != null && retry > 0) {
            // Honor a positive Retry-After, but never sleep longer than our own ceiling — a huge or
            // hostile value must not stall the caller for hours. Not jittered: the server asked for
            // this exact delay. A zero/past value falls through to the jittered exponential backoff
            // so we never retry-storm with no delay.
            seconds = Math.min(MAX_RETRY_AFTER_SECONDS, retry);
        } else {
            seconds = jitter(Math.min(MAX_BACKOFF_SECONDS, 0.5 * Math.pow(2, attempt)));
        }
        sleeper.sleep(seconds);
    }

    /**
     * Parse a {@code Retry-After} header into whole seconds. Supports the delay-seconds form
     * ({@code 120}) and the HTTP-date form ({@code Wed, 21 Oct 2015 07:28:00 GMT}). Returns null
     * when absent/unparseable; never negative.
     */
    private Integer parseRetryAfter(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("[0-9]+")) {
            try {
                return (int) Math.max(0, Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            ZonedDateTime when = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME);
            long delta = when.toEpochSecond() - Instant.now().getEpochSecond();
            return (int) Math.max(0, delta);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Add a small upward jitter (0-25%) so correlated clients don't retry/poll in lockstep.
     * Upward-only, so a jittered delay is never shorter than requested.
     */
    private double jitter(double seconds) {
        return seconds + seconds * 0.25 * ThreadLocalRandom.current().nextDouble();
    }

    /**
     * Whether a request is safe to auto-retry after a 5xx or network failure. GET, HEAD, PUT,
     * DELETE, OPTIONS and TRACE are idempotent by HTTP semantics; a request of any method carrying
     * an {@code Idempotency-Key} is retry-safe too (the backend deduplicates it). Everything else —
     * notably a bare POST — is not, so a transient error surfaces as an exception instead of risking
     * a duplicate job.
     */
    private boolean isIdempotent(Request request) {
        if (IDEMPOTENT_METHODS.contains(request.method().toUpperCase())) {
            return true;
        }
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            if (header.getKey().equalsIgnoreCase("Idempotency-Key")
                    && header.getValue() != null && !header.getValue().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static void closeQuietly(InputStream in) {
        try {
            in.close();
        } catch (IOException ignored) {
            // best effort; we are discarding this response to retry
        }
    }
}
