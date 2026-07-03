package com.api2convert.exception;

import java.util.Map;

/**
 * The API key was missing, invalid or not permitted (HTTP 401 / 403).
 */
public class AuthenticationException extends ApiException {

    public AuthenticationException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message, statusCode, requestId, body);
    }
}
