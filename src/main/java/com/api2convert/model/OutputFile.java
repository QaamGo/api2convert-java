package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * A produced output file. {@link #uri()} is a self-contained download URL (no auth), valid for a
 * limited time (24h by default).
 */
public record OutputFile(
        String id,
        String uri,
        String filename,
        Long size,
        String status,
        String contentType,
        String checksum,
        Map<String, Object> metadata) {

    public static OutputFile fromMap(Map<String, Object> data) {
        return new OutputFile(
                Data.nullableString(data.get("id")),
                Data.string(data.get("uri")),
                Data.nullableString(data.get("filename")),
                Data.nullableLong(data.get("size")),
                Data.nullableString(data.get("status")),
                Data.nullableString(data.get("content_type")),
                Data.nullableString(data.get("checksum")),
                Data.object(data.get("metadata")));
    }

    /** Convenience factory for building an output reference by hand (e.g. from the Jobs API). */
    public static OutputFile of(String id, String uri, String filename) {
        return new OutputFile(id, uri, filename, null, null, null, null, Map.of());
    }
}
