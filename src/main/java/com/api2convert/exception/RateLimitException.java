package com.api2convert.exception;

import java.util.Map;

/**
 * Too many requests (HTTP 429). The client already retries these automatically with backoff;
 * this is thrown only once retries are exhausted.
 */
public class RateLimitException extends ApiException {

    private final Integer retryAfter;

    /**
     * @param retryAfter seconds to wait before retrying, from the {@code Retry-After} header, if provided
     */
    public RateLimitException(String message, int statusCode, String requestId,
                              Map<String, Object> body, Integer retryAfter) {
        super(message, statusCode, requestId, body);
        this.retryAfter = retryAfter;
    }

    /** Seconds to wait before retrying, from {@code Retry-After}; may be null. */
    public Integer getRetryAfter() {
        return retryAfter;
    }
}
