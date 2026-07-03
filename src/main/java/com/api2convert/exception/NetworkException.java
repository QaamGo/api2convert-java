package com.api2convert.exception;

/**
 * A request did not yield a usable response. Two throw sites:
 *
 * <ul>
 *   <li>a transport-level failure (DNS, connection, TLS or read failure) — retried automatically
 *       for idempotent requests and thrown once retries are exhausted;</li>
 *   <li>a successful (2xx) response whose body is not valid JSON (e.g. an intermediary HTML/error
 *       page) — thrown directly by the decoder and not retried.</li>
 * </ul>
 */
public class NetworkException extends Api2ConvertException {

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
