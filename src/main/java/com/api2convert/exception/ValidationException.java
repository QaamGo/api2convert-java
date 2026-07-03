package com.api2convert.exception;

import java.util.Map;

/**
 * The request was invalid — e.g. an unknown target format or an illegal option (HTTP 400 / 422).
 */
public class ValidationException extends ApiException {

    public ValidationException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message, statusCode, requestId, body);
    }
}
