package com.api2convert.enums;

/**
 * The kinds of source an input file can be created from — the values of the API's input
 * {@code type} field. Provided as a typed reference for building input descriptors by hand, e.g.
 * {@code addInput(id, Map.of("type", InputType.REMOTE.wire(), "source", "https://..."))}; the
 * descriptor maps the SDK sends use these string values.
 */
public enum InputType {
    /** A file uploaded directly to the per-job upload server. */
    UPLOAD("upload"),
    /** A file fetched by the API from a public URL. */
    REMOTE("remote"),
    /** The output of a previous conversion in the same job. */
    OUTPUT("output"),
    /** A finished output of another job, by its id (job chaining). */
    INPUT_ID("input_id"),
    /** A file picked through the Google Drive picker. */
    GDRIVE_PICKER("gdrive_picker"),
    /** A small file embedded inline as base64. */
    BASE64("base64"),
    /** A file imported from cloud storage (S3, GCS, Azure, FTP, ...). */
    CLOUD("cloud");

    private final String wire;

    InputType(String wire) {
        this.wire = wire;
    }

    /** The API's string value for this input type. */
    public String wire() {
        return wire;
    }

    /** Resolve a raw type string to a case, or null if unknown. */
    public static InputType fromWire(String type) {
        if (type != null) {
            for (InputType t : values()) {
                if (t.wire.equals(type)) {
                    return t;
                }
            }
        }
        return null;
    }
}
