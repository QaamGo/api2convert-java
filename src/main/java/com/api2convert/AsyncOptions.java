package com.api2convert;

/**
 * Optional controls for {@link Api2Convert#convertAsync}.
 *
 * <p>Fluent: {@code new AsyncOptions().callback("https://app.example.com/hook")}.
 */
public final class AsyncOptions {

    String callback;
    String category;
    String filename;
    String downloadPassword;

    /** Webhook URL notified when the job's status changes (also sets {@code notify_status}). */
    public AsyncOptions callback(String callback) {
        this.callback = callback;
        return this;
    }

    /** Conversion category, when a target is ambiguous. */
    public AsyncOptions category(String category) {
        this.category = category;
        return this;
    }

    /** Filename to advertise for an uploaded local file. */
    public AsyncOptions filename(String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * Protect the result with this password. The returned {@link com.api2convert.model.Job} is not a
     * result wrapper, so a later download must supply the {@code X-Api2convert-Download-Password} header.
     */
    public AsyncOptions downloadPassword(String downloadPassword) {
        this.downloadPassword = downloadPassword;
        return this;
    }
}
