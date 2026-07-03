package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * An input file attached to a job.
 */
public record InputFile(
        String id,
        String type,
        String source,
        String status,
        String filename,
        Long size,
        String contentType,
        Map<String, Object> options) {

    public static InputFile fromMap(Map<String, Object> data) {
        return new InputFile(
                Data.nullableString(data.get("id")),
                Data.string(data.get("type")),
                Data.nullableString(data.get("source")),
                Data.nullableString(data.get("status")),
                Data.nullableString(data.get("filename")),
                Data.nullableLong(data.get("size")),
                Data.nullableString(data.get("content_type")),
                Data.object(data.get("options")));
    }
}
