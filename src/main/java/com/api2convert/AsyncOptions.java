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
    java.util.List<com.api2convert.model.OutputTarget> outputTargets;

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

    /**
     * Deliver the result to one or more cloud-storage targets. The targets attach to the
     * conversion's {@code output_target} and are never merged into the conversion options map.
     */
    public AsyncOptions outputTargets(com.api2convert.model.OutputTarget... outputTargets) {
        this.outputTargets = java.util.List.of(outputTargets);
        return this;
    }

    /** List form of {@link #outputTargets(com.api2convert.model.OutputTarget...)}. */
    public AsyncOptions outputTargets(java.util.List<com.api2convert.model.OutputTarget> outputTargets) {
        this.outputTargets = outputTargets;
        return this;
    }
}
