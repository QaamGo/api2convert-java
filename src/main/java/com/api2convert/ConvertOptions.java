package com.api2convert;

/**
 * Optional, less-common controls for {@link Api2Convert#convert}. Kept separate from the open-ended
 * conversion-options map so SDK controls can never collide with an API option key.
 *
 * <p>Fluent: {@code new ConvertOptions().category("image").downloadPassword("hunter2")}.
 */
public final class ConvertOptions {

    String category;
    Integer timeout;
    Integer outputIndex;
    String filename;
    String downloadPassword;
    java.util.List<com.api2convert.model.OutputTarget> outputTargets;

    /** Conversion category, when a target is ambiguous. */
    public ConvertOptions category(String category) {
        this.category = category;
        return this;
    }

    /** Override the poll timeout (seconds). */
    public ConvertOptions timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /** Which output file the result exposes (default 0). */
    public ConvertOptions outputIndex(int outputIndex) {
        this.outputIndex = outputIndex;
        return this;
    }

    /** Filename to advertise for an uploaded local file. */
    public ConvertOptions filename(String filename) {
        this.filename = filename;
        return this;
    }

    /** Protect the result with this password; remembered and sent automatically on download. */
    public ConvertOptions downloadPassword(String downloadPassword) {
        this.downloadPassword = downloadPassword;
        return this;
    }

    /**
     * Deliver the result to one or more cloud-storage targets instead of a downloadable output. The
     * targets attach to the conversion's {@code output_target}; they are never merged into the
     * conversion options map. When set, the conversion has no local output and {@code convert()}
     * returns the completed job without downloading.
     */
    public ConvertOptions outputTargets(com.api2convert.model.OutputTarget... outputTargets) {
        this.outputTargets = java.util.List.of(outputTargets);
        return this;
    }

    /** List form of {@link #outputTargets(com.api2convert.model.OutputTarget...)}. */
    public ConvertOptions outputTargets(java.util.List<com.api2convert.model.OutputTarget> outputTargets) {
        this.outputTargets = outputTargets;
        return this;
    }
}
