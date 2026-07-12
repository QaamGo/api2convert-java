package com.api2convert.model;

import com.api2convert.enums.CloudProvider;
import com.api2convert.support.Data;
import com.api2convert.support.Redaction;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cloud-storage output target — delivers a conversion's result to customer-owned storage. Attach
 * one (or several) to a conversion via {@code convert(..., new ConvertOptions().outputTargets(t))}
 * or a raw {@code jobs().create} conversion map.
 *
 * <p>Generic only this wave: a {@link CloudProvider} {@code type} plus free-form {@code parameters}
 * and {@code credentials} (per-provider output factories live in a separate service and diverge per
 * provider). It serializes {@code {type, parameters, credentials}} and <strong>omits {@code status}</strong>
 * on create ({@code status} is server-set and read-only). On read, {@code type}, {@code parameters}
 * and {@code status} round-trip as raw values; {@code credentials} are <strong>never</strong> hydrated
 * (the API returns them empty and the SDK does not surface them).
 *
 * <p>When any output target is set the conversion produces no local output — {@code convert()} returns
 * the completed job without downloading.
 */
public record OutputTarget(
        String type,
        Map<String, Object> parameters,
        Map<String, Object> credentials,
        String status) {

    public OutputTarget {
        parameters = parameters != null ? parameters : Map.of();
        credentials = credentials != null ? credentials : Map.of();
    }

    /** Build an output target for a provider with free-form {@code parameters} / {@code credentials}. */
    public static OutputTarget of(CloudProvider type, Map<String, Object> parameters, Map<String, Object> credentials) {
        return new OutputTarget(type.wire(), parameters, credentials, null);
    }

    /** Build an output target keyed by a raw provider string. */
    public static OutputTarget of(String type, Map<String, Object> parameters, Map<String, Object> credentials) {
        return new OutputTarget(type, parameters, credentials, null);
    }

    /**
     * Hydrate from a decoded {@code output_target[]} element. {@code type} and {@code status} stay
     * raw strings (an unknown provider round-trips untyped); {@code credentials} are not surfaced.
     */
    public static OutputTarget fromMap(Map<String, Object> data) {
        return new OutputTarget(
                Data.nullableString(data.get("type")),
                Data.object(data.get("parameters")),
                Map.of(),
                Data.nullableString(data.get("status")));
    }

    /** The wire descriptor sent on create: {@code {type, parameters, credentials}} — never {@code status}. */
    public Map<String, Object> toDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("type", type);
        descriptor.put("parameters", parameters);
        descriptor.put("credentials", credentials);
        return descriptor;
    }

    @Override
    public String toString() {
        return "OutputTarget{type=" + type
                + ", parameters=" + Redaction.maskSensitive(parameters)
                + ", credentials=" + Redaction.REDACTED
                + ", status=" + status + "}";
    }
}
