package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.api2convert.exception.NetworkException;
import com.api2convert.exception.ServerException;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetryTest extends A2CTestCase {

    private Config retries(int n) {
        return Config.builder().maxRetries(n).build();
    }

    @Test
    void retriesTransientStatusThenSucceeds() {
        http.addJson(503, "{\"message\":\"temporary\"}");
        http.addJson(429, "{\"message\":\"slow down\"}");
        http.addJson(200, "{\"id\":\"job-1\",\"status\":{\"code\":\"completed\"}}");

        Job job = client(retries(2)).jobs().get("job-1");

        assertEquals("job-1", job.id());
        assertEquals(3, http.requests.size());
    }

    @Test
    void retriesNetworkErrorThenSucceeds() {
        http.addException(new IOException("connection reset"));
        http.addJson(200, "{\"id\":\"job-2\",\"status\":{\"code\":\"completed\"}}");

        Job job = client(retries(1)).jobs().get("job-2");

        assertEquals("job-2", job.id());
        assertEquals(2, http.requests.size());
    }

    @Test
    void networkErrorRetriesExhaustedThrowsNetworkExceptionWithCause() {
        IOException cause = new IOException("connection reset");
        http.addException(cause);
        http.addException(cause);

        NetworkException e = assertThrows(NetworkException.class,
                () -> client(retries(1)).jobs().get("job-x"));
        assertSame(cause, e.getCause(), "The original transport error must be chained.");
        assertEquals(2, http.requests.size());
    }

    @Test
    void createJobIsNotRetriedOnServerError() {
        // POST /jobs is non-idempotent: a 5xx must surface, not silently retry into a possible
        // duplicate job (and a duplicate charge).
        http.addJson(503, "{\"message\":\"temporary\"}");

        assertThrows(ServerException.class,
                () -> client(retries(2)).jobs().create(Map.of("conversion", List.of(Map.of("target", "pdf")))));
        assertEquals(1, http.requests.size(), "A bare POST must be sent exactly once on 5xx.");
    }

    @Test
    void createJobWithIdempotencyKeyIsRetriedOnServerError() {
        http.addJson(503, "{\"message\":\"temporary\"}");
        http.addJson(200, "{\"id\":\"job-1\",\"status\":{\"code\":\"completed\"}}");

        Job job = client(retries(2)).jobs()
                .create(Map.of("conversion", List.of(Map.of("target", "pdf"))), "idem-1");

        assertEquals("job-1", job.id());
        assertEquals(2, http.requests.size());
        assertEquals("idem-1", requestAt(0).header("Idempotency-Key"));
    }
}
