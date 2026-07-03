package com.api2convert.exception;

/**
 * Base class for every exception thrown by the SDK.
 *
 * <p>Catch this to handle any SDK failure in one place; catch a more specific subclass
 * (e.g. {@link RateLimitException}, {@link ConversionFailedException}) to react to a particular
 * failure mode. All SDK exceptions are unchecked.
 */
public class Api2ConvertException extends RuntimeException {

    public Api2ConvertException(String message) {
        super(message);
    }

    public Api2ConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
