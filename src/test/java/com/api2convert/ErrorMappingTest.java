package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.api2convert.exception.ApiException;
import com.api2convert.exception.AuthenticationException;
import com.api2convert.exception.NetworkException;
import com.api2convert.exception.NotFoundException;
import com.api2convert.exception.PaymentRequiredException;
import com.api2convert.exception.RateLimitException;
import com.api2convert.exception.ServerException;
import com.api2convert.exception.ValidationException;
import com.api2convert.http.Config;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class ErrorMappingTest extends A2CTestCase {

    private Config noRetries() {
        return Config.builder().maxRetries(0).build();
    }

    static Stream<Arguments> statuses() {
        return Stream.of(
                Arguments.of(400, ValidationException.class),
                Arguments.of(401, AuthenticationException.class),
                Arguments.of(402, PaymentRequiredException.class),
                Arguments.of(403, AuthenticationException.class),
                Arguments.of(404, NotFoundException.class),
                Arguments.of(422, ValidationException.class),
                Arguments.of(418, ApiException.class));
    }

    @ParameterizedTest
    @MethodSource("statuses")
    void statusMapsToTypedException(int status, Class<? extends ApiException> expected) {
        http.addJson(status, "{\"message\":\"boom\"}", Map.of("X-Request-Id", "req-42"));

        ApiException e = assertThrows(ApiException.class, () -> client(noRetries()).jobs().get("job-x"));
        assertInstanceOf(expected, e);
        assertEquals(status, e.getStatusCode());
        assertEquals("boom", e.getMessage());
        assertEquals("req-42", e.getRequestId());
    }

    @Test
    void rateLimitExposesRetryAfterAfterRetriesExhausted() {
        http.addJson(429, "{\"message\":\"slow down\"}", Map.of("Retry-After", "7"));

        RateLimitException e = assertThrows(RateLimitException.class,
                () -> client(noRetries()).jobs().get("job-x"));
        assertEquals(429, e.getStatusCode());
        assertEquals(7, e.getRetryAfter());
    }

    @Test
    void serverErrorMapsToServerException() {
        http.addJson(503, "{\"message\":\"maintenance\"}");

        assertThrows(ServerException.class, () -> client(noRetries()).jobs().get("job-x"));
    }

    @Test
    void fallsBackToAMessageWhenNoBodyMessage() {
        http.addRaw(404, new byte[0]);

        NotFoundException e = assertThrows(NotFoundException.class,
                () -> client(noRetries()).jobs().get("job-x"));
        assertFalse(e.getMessage().isEmpty());
    }

    @Test
    void nonJsonSuccessBodyRaisesNetworkException() {
        // A 2xx carrying a non-JSON body (e.g. an intermediary HTML/error page) must stay inside the
        // SDK exception hierarchy, not leak a bare parse error.
        http.addRaw(200, "<html>maintenance</html>".getBytes());

        assertThrows(NetworkException.class, () -> client(noRetries()).jobs().get("job-x"));
    }
}
