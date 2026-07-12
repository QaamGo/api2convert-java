package com.api2convert.enums;

/**
 * The customer cloud-storage providers the API can import inputs from and deliver outputs to — the
 * values of a cloud descriptor's {@code source} (input) / {@code type} (output) field.
 *
 * <p><strong>Build-side vocabulary only.</strong> Use it to construct cloud descriptors with the
 * right provider string (via {@link CloudProvider#wire()}). Read models keep {@code source} /
 * {@code type} / {@code status} as raw strings — an unknown provider string from the server must
 * round-trip untyped and never throw, so this enum is deliberately not used when hydrating.
 *
 * <p>{@code AMAZON_S3}, {@code AZURE}, {@code FTP} and {@code GOOGLE_CLOUD} work as both input and
 * output; {@code GDRIVE} and {@code YOUTUBE} are output-only (they validate as an input source but
 * have no downloader).
 */
public enum CloudProvider {
    AMAZON_S3("amazons3"),
    AZURE("azure"),
    FTP("ftp"),
    GDRIVE("gdrive"),
    GOOGLE_CLOUD("googlecloud"),
    YOUTUBE("youtube");

    private final String wire;

    CloudProvider(String wire) {
        this.wire = wire;
    }

    /** The API's string value for this provider (flat/lowercase, e.g. {@code amazons3}). */
    public String wire() {
        return wire;
    }

    /** Resolve a raw provider string to a case, or null if unknown (callers must tolerate null). */
    public static CloudProvider fromWire(String provider) {
        if (provider != null) {
            for (CloudProvider p : values()) {
                if (p.wire.equals(provider)) {
                    return p;
                }
            }
        }
        return null;
    }
}
