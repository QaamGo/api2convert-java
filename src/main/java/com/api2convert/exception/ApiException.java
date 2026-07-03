package com.api2convert.exception;

import java.util.Map;

/**
 * Thrown when the API returns an HTTP error response (status &gt;= 400).
 *
 * <p>Specific status codes map to dedicated subclasses ({@link AuthenticationException},
 * {@link ValidationException}, {@link RateLimitException}, {@link NotFoundException},
 * {@link PaymentRequiredException}, {@link ServerException}); this base type is used for any 4xx
 * that has no more specific subclass.
 */
public class ApiException extends Api2ConvertException {

    private final int statusCode;
    private final String requestId;
    private final transient Map<String, Object> body;

    /**
     * @param message    error message from the API (the {@code message} field) or a fallback
     * @param statusCode the HTTP status code of the response
     * @param requestId  value of the {@code X-Request-Id} header, if any (quote it in support requests)
     * @param body       the decoded JSON error body, when available
     */
    public ApiException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message);
        this.statusCode = statusCode;
        this.requestId = requestId;
        this.body = body != null ? body : Map.of();
    }

    /** The HTTP status code of the error response. */
    public int getStatusCode() {
        return statusCode;
    }

    /** The {@code X-Request-Id} header value, if the response carried one. May be null. */
    public String getRequestId() {
        return requestId;
    }

    /** The decoded JSON error body (never null; empty when the body was absent or not JSON). */
    public Map<String, Object> getBody() {
        return body;
    }
}
