package com.api2convert.exception;

/**
 * A webhook payload could not be verified or parsed: a missing or wrong signature, or a body that
 * is not a valid JSON object.
 */
public class SignatureVerificationException extends Api2ConvertException {

    public SignatureVerificationException(String message) {
        super(message);
    }
}
