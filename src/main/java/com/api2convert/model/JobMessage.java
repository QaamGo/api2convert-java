package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * An error or warning attached to a job (the {@code errors[]} / {@code warnings[]} entries).
 */
public record JobMessage(
        Integer code,
        String message,
        String source,
        String idSource,
        Map<String, Object> details) {

    public static JobMessage fromMap(Map<String, Object> data) {
        return new JobMessage(
                Data.nullableInt(data.get("code")),
                Data.string(data.get("message")),
                Data.nullableString(data.get("source")),
                Data.nullableString(data.get("id_source")),
                Data.object(data.get("details")));
    }
}
