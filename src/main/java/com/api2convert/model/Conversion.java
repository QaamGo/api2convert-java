package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * A single conversion within a job: the target format plus its options.
 */
public record Conversion(
        String target,
        String id,
        String category,
        Map<String, Object> options,
        Map<String, Object> metadata) {

    public static Conversion fromMap(Map<String, Object> data) {
        return new Conversion(
                Data.string(data.get("target")),
                Data.nullableString(data.get("id")),
                Data.nullableString(data.get("category")),
                Data.object(data.get("options")),
                Data.object(data.get("metadata")));
    }
}
