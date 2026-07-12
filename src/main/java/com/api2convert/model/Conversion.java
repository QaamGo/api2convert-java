package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.List;
import java.util.Map;

/**
 * A single conversion within a job: the target format plus its options.
 *
 * <p>{@link #outputTargets()} carries any cloud {@code output_target[]} the conversion delivers its
 * result to; it is empty for an ordinary (downloadable) conversion.
 */
public record Conversion(
        String target,
        String id,
        String category,
        Map<String, Object> options,
        Map<String, Object> metadata,
        List<OutputTarget> outputTargets) {

    /**
     * ABI-safe delegating constructor preserving the pre-cloud 5-arg shape (no output targets).
     * Keeps existing call sites compiling/linking after {@code outputTargets} was appended.
     */
    public Conversion(String target, String id, String category,
                      Map<String, Object> options, Map<String, Object> metadata) {
        this(target, id, category, options, metadata, List.of());
    }

    public static Conversion fromMap(Map<String, Object> data) {
        return new Conversion(
                Data.string(data.get("target")),
                Data.nullableString(data.get("id")),
                Data.nullableString(data.get("category")),
                Data.object(data.get("options")),
                Data.object(data.get("metadata")),
                Data.mapObjects(data.get("output_target"), OutputTarget::fromMap));
    }
}
