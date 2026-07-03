package com.api2convert.exception;

import java.util.Map;

/**
 * The account has no remaining quota or its contract does not cover the request (HTTP 402).
 */
public class PaymentRequiredException extends ApiException {

    public PaymentRequiredException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message, statusCode, requestId, body);
    }
}
