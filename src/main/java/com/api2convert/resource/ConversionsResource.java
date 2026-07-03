package com.api2convert.resource;

import com.api2convert.http.Transport;
import com.api2convert.support.Data;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The conversions catalog ({@code GET /conversions}) — the source of truth for which targets exist
 * and which options each accepts. No authentication needed.
 *
 * <p>Use {@link #options} to discover the valid {@code options} for a target before building a
 * conversion.
 */
public final class ConversionsResource {

    private final Transport transport;

    public ConversionsResource(Transport transport) {
        this.transport = transport;
    }

    /**
     * List supported conversions, optionally filtered by category and/or target. Each entry is a
     * map: {@code { id, category, target, options }}.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String category, String target, int page) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        if (category != null) {
            query.put("category", category);
        }
        if (target != null) {
            query.put("target", target);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object row : Data.list(transport.request("GET", "/conversions", null, query))) {
            if (row instanceof Map) {
                rows.add((Map<String, Object>) row);
            }
        }
        return rows;
    }

    public List<Map<String, Object>> list() {
        return list(null, null, 1);
    }

    /**
     * The option schema (type / enum / default / range) for a single target. {@code category} is
     * optional — pass it only to disambiguate an ambiguous target.
     */
    public Map<String, Object> options(String target, String category) {
        List<Map<String, Object>> rows = list(category, target, 1);
        Map<String, Object> first = rows.isEmpty() ? Map.of() : rows.get(0);
        return Data.object(first.get("options"));
    }

    public Map<String, Object> options(String target) {
        return options(target, null);
    }
}
