package com.api2convert.model;

import com.api2convert.support.Data;
import java.util.Map;

/**
 * A saved conversion preset (a reusable named set of target + options).
 */
public record Preset(
        String id,
        String name,
        String target,
        String category,
        String scope,
        Map<String, Object> options) {

    public static Preset fromMap(Map<String, Object> data) {
        return new Preset(
                Data.nullableString(data.get("id")),
                Data.string(data.get("name")),
                Data.nullableString(data.get("target")),
                Data.nullableString(data.get("category")),
                Data.nullableString(data.get("scope")),
                Data.object(data.get("options")));
    }
}
