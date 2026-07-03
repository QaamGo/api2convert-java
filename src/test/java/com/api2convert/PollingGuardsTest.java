package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.ConversionFailedException;
import com.api2convert.http.Config;
import com.api2convert.http.Request;
import com.api2convert.http.Response;
import com.api2convert.http.Transport;
import com.api2convert.model.Job;
import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Guards that keep the SDK from hammering the API — the reliability lessons the legacy onlineconvert
 * SDK learned the hard way (interval floor, timeout ceiling, bounded Retry-After, no-retry for a
 * non-replayable body or a non-idempotent POST, terminal-state completeness).
 */
class PollingGuardsTest extends A2CTestCase {

    private static final String URL = "https://api.api2convert.com/v2/jobs";

    private Transport transport(int maxRetries) {
        return new Transport(http, Config.builder().maxRetries(maxRetries).build(), "k", seconds -> slept.add(seconds));
    }

    @Test
    void pollIntervalIsFlooredToAMinimum() {
        assertTrue(Config.builder().pollInterval(0.0).build().pollInterval() >= Config.MIN_POLL_INTERVAL);
        assertTrue(Config.builder().pollInterval(-5.0).build().pollInterval() >= Config.MIN_POLL_INTERVAL);
    }

    @Test
    void pollMaxIntervalIsNeverBelowTheStartInterval() {
        Config config = Config.builder().pollInterval(3.0).pollMaxInterval(1.0).build();
        assertTrue(config.pollMaxInterval() >= config.pollInterval());
    }

    @Test
    void pollTimeoutIsCappedToAMaximum() {
        assertEquals(Config.MAX_POLL_TIMEOUT, Config.builder().pollTimeout(Integer.MAX_VALUE).build().pollTimeout());
    }

    @Test
    void timeoutIsNeverDisabled() {
        // A per-request timeout of 0 means "no timeout" — the classic unbounded-hang landmine.
        assertTrue(Config.builder().timeout(0).build().timeout() >= 1);
    }

    @Test
    void misconfiguredZeroIntervalIsFlooredNotBusyLooped() {
        Api2Convert client = client(Config.builder().pollInterval(0.0).build());
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"incomplete\"}}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        Job job = client.jobs().await("j");

        assertTrue(job.isCompleted());
        assertFalse(slept.isEmpty(), "await() must have paused at least once between polls.");
        for (double interval : slept) {
            assertTrue(interval >= Config.MIN_POLL_INTERVAL);
        }
    }

    @Test
    void canceledJobRaisesConversionFailed() {
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"canceled\"}}");
        assertThrows(ConversionFailedException.class, () -> client().jobs().await("j"));
    }

    @Test
    void canceledJobIsTerminalWhenThrowOnFailureDisabled() {
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"canceled\"}}");
        Job job = client().jobs().await("j", null, false);
        assertTrue(job.isCanceled());
        assertTrue(job.isTerminal());
    }

    @Test
    void honoredRetryAfterIsClampedToCeiling() {
        Api2Convert client = client(Config.builder().maxRetries(1).build());
        http.addJson(429, "{\"message\":\"slow\"}", Map.of("Retry-After", "99999"));
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        client.jobs().get("j");

        assertEquals(1, slept.size());
        assertEquals(120.0, slept.get(0));
    }

    @Test
    void retryAfterHttpDateIsParsedAndClamped() {
        Api2Convert client = client(Config.builder().maxRetries(1).build());
        String future = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                ZonedDateTime.now(ZoneId.of("GMT")).plusSeconds(3600));
        http.addJson(503, "{\"message\":\"busy\"}", Map.of("Retry-After", future));
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        client.jobs().get("j");

        assertEquals(1, slept.size());
        assertEquals(120.0, slept.get(0));
    }

    @Test
    void retryAfterZeroFallsBackToBackoffInsteadOfHammering() {
        Api2Convert client = client(Config.builder().maxRetries(1).build());
        http.addJson(503, "{\"message\":\"busy\"}", Map.of("Retry-After", "0"));
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        client.jobs().get("j");

        assertEquals(1, slept.size());
        assertTrue(slept.get(0) >= 0.5);
    }

    @Test
    void nonReplayableBodyIsNotRetried() throws Exception {
        // Use 429 (retryable for ANY method) so idempotency is not what blocks the retry — the
        // one-shot streamed body is the only thing that can. This keeps it a real guard: relax the
        // replayable gate and it would retry to 200.
        Transport transport = transport(3);
        Request request = Request.streaming("POST", URL, Map.of(),
                () -> new ByteArrayInputStream("binary-upload-payload".getBytes()));

        http.addJson(429, "{\"message\":\"slow\"}");
        http.addJson(429, "{\"message\":\"slow\"}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        Response response = transport.send(request);

        assertEquals(429, response.status());
        assertEquals(1, http.requests.size(), "A non-replayable body must be sent exactly once.");
    }

    @Test
    void seekableNonIdempotentPostIsNotRetriedOnServerError() {
        // A byte-array body is replayable, but a bare POST is not idempotent: retrying a 5xx could
        // create a duplicate job. Replayability alone must not enable retry.
        Transport transport = transport(2);
        Request request = Request.of("POST", URL, Map.of(), "payload".getBytes(), false);

        http.addJson(503, "{\"message\":\"temp\"}");
        http.addJson(503, "{\"message\":\"temp\"}");

        Response response = transport.send(request);

        assertEquals(503, response.status());
        assertEquals(1, http.requests.size(), "A non-idempotent POST must not be auto-retried on 5xx.");
    }

    @Test
    void postWithIdempotencyKeyIsRetriedOnServerError() {
        Transport transport = transport(2);
        Request request = Request.of("POST", URL, Map.of("Idempotency-Key", "key-123"), "payload".getBytes(), false);

        http.addJson(503, "{\"message\":\"temp\"}");
        http.addJson(503, "{\"message\":\"temp\"}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        Response response = transport.send(request);

        assertEquals(200, response.status());
        assertEquals(3, http.requests.size());
    }

    @Test
    void rateLimitedPostIsRetriedEvenWithoutIdempotencyKey() {
        // A 429 is rejected before processing, so it is safe to retry for any method.
        Transport transport = transport(1);
        Request request = Request.of("POST", URL, Map.of(), "payload".getBytes(), false);

        http.addJson(429, "{\"message\":\"slow\"}");
        http.addJson(200, "{\"id\":\"j\",\"status\":{\"code\":\"completed\"}}");

        Response response = transport.send(request);

        assertEquals(200, response.status());
        assertEquals(2, http.requests.size());
    }
}
