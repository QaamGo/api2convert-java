package com.api2convert.exception;

import java.util.Map;

/**
 * The requested resource does not exist (HTTP 404).
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message, statusCode, requestId, body);
    }
}
