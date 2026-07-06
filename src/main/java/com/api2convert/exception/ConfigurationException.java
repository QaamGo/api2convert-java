package com.api2convert.exception;

/**
 * The client was misconfigured — e.g. constructed without an API key (and none available in the
 * {@code API2CONVERT_API_KEY} environment variable). Thrown at construction time, before any request
 * is made, so it always surfaces inside the {@link Api2ConvertException} hierarchy like every other
 * SDK failure.
 */
public class ConfigurationException extends Api2ConvertException {

    public ConfigurationException(String message) {
        super(message);
    }
}
