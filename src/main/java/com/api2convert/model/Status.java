package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * A job's status: a machine-readable {@link #code()} plus an optional human {@link #info()}.
 */
public record Status(String code, String info) {

    public static Status fromMap(Map<String, Object> data) {
        return new Status(
                Data.string(data.get("code")),
                Data.nullableString(data.get("info")));
    }
}
