package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * An input file attached to a job.
 *
 * <p>{@code source}, {@code type} and {@code status} are raw strings (a cloud input's
 * {@code source} is a provider string such as {@code amazons3}, and an unknown provider round-trips
 * untyped). {@link #parameters()} surfaces a cloud input's {@code parameters}; {@code credentials}
 * are never hydrated (the API returns them empty).
 */
public record InputFile(
        String id,
        String type,
        String source,
        String status,
        String filename,
        Long size,
        String contentType,
        Map<String, Object> options,
        Map<String, Object> parameters) {

    /**
     * ABI-safe delegating constructor preserving the pre-cloud 8-arg shape (no cloud parameters).
     * Keeps existing call sites compiling/linking after {@code parameters} was appended.
     */
    public InputFile(String id, String type, String source, String status, String filename,
                     Long size, String contentType, Map<String, Object> options) {
        this(id, type, source, status, filename, size, contentType, options, Map.of());
    }

    public static InputFile fromMap(Map<String, Object> data) {
        return new InputFile(
                Data.nullableString(data.get("id")),
                Data.string(data.get("type")),
                Data.nullableString(data.get("source")),
                Data.nullableString(data.get("status")),
                Data.nullableString(data.get("filename")),
                Data.nullableLong(data.get("size")),
                Data.nullableString(data.get("content_type")),
                Data.object(data.get("options")),
                Data.object(data.get("parameters")));
    }
}
