package com.api2convert.exception;

import java.util.Map;

/**
 * The API encountered a server-side error (HTTP 5xx). Retried automatically for idempotent
 * requests; thrown once retries are exhausted.
 */
public class ServerException extends ApiException {

    public ServerException(String message, int statusCode, String requestId, Map<String, Object> body) {
        super(message, statusCode, requestId, body);
    }
}
