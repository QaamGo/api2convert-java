package com.api2convert.resource;

import com.api2convert.http.Transport;
import com.api2convert.model.Preset;
import com.api2convert.support.Data;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saved conversion presets (reusable named target + options).
 */
public final class PresetsResource {

    private final Transport transport;

    public PresetsResource(Transport transport) {
        this.transport = transport;
    }

    public List<Preset> list(String category, String target, String filter) {
        Map<String, String> query = new LinkedHashMap<>();
        if (category != null) {
            query.put("category", category);
        }
        if (target != null) {
            query.put("target", target);
        }
        if (filter != null) {
            query.put("filter", filter);
        }
        return Data.mapObjects(transport.request("GET", "/presets", null, query), Preset::fromMap);
    }

    public List<Preset> list() {
        return list(null, null, null);
    }

    /** @param payload {@code { name, target, options, scope?, category? }} */
    public Preset create(Map<String, Object> payload) {
        return Preset.fromMap(Data.object(transport.request("POST", "/presets", payload)));
    }

    public Preset get(String presetId) {
        return Preset.fromMap(Data.object(transport.request("GET", "/presets/" + presetId)));
    }

    public Preset update(String presetId, Map<String, Object> payload) {
        return Preset.fromMap(Data.object(transport.request("PATCH", "/presets/" + presetId, payload)));
    }

    public void delete(String presetId) {
        transport.request("DELETE", "/presets/" + presetId);
    }
}
