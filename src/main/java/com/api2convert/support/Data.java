package com.api2convert.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Typed accessors over a decoded JSON object ({@code Map<String, Object>} produced by Jackson).
 *
 * <p>Keeps model hydration null-safe and free of scattered casts. Every accessor tolerates a
 * missing, null or wrong-typed value and returns a default rather than throwing — models must
 * never fail to hydrate on a surprising payload. Internal helper, not part of the public API.
 */
public final class Data {

    private Data() {
    }

    public static String string(Object value, String defaultValue) {
        return value instanceof String s ? s : defaultValue;
    }

    public static String string(Object value) {
        return string(value, "");
    }

    public static String nullableString(Object value) {
        return value instanceof String s ? s : null;
    }

    /**
     * Coerce a value to an {@link Integer}, or null. Accepts JSON numbers and numeric strings;
     * mirrors PHP {@code is_numeric()} by explicitly rejecting booleans (a boolean is not numeric).
     */
    public static Integer nullableInt(Object value) {
        if (value instanceof Boolean) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            String t = s.trim();
            try {
                return (int) Long.parseLong(t);
            } catch (NumberFormatException ignored) {
                try {
                    return (int) Double.parseDouble(t);
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Coerce a value to a {@link Long}, or null. Used for byte sizes, which can exceed a 32-bit int.
     */
    public static Long nullableLong(Object value) {
        if (value instanceof Boolean) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            String t = s.trim();
            try {
                return Long.parseLong(t);
            } catch (NumberFormatException ignored) {
                try {
                    return (long) Double.parseDouble(t);
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean bool(Object value, boolean defaultValue) {
        return value instanceof Boolean b ? b : defaultValue;
    }

    /**
     * Return {@code value} if it is a JSON object, otherwise an empty map.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    /**
     * Return {@code value} as a list. A JSON array passes through; a JSON object is reduced to its
     * values (mirroring PHP {@code array_values}); anything else becomes an empty list.
     */
    public static List<Object> list(Object value) {
        if (value instanceof List<?> l) {
            return new ArrayList<>(l);
        }
        if (value instanceof Map<?, ?> m) {
            return new ArrayList<>(m.values());
        }
        return List.of();
    }

    /**
     * Map each JSON-object element of {@code value} through {@code factory}, skipping non-objects.
     * The returned list is unmodifiable.
     *
     * @param <T> the model type produced by the factory
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> mapObjects(Object value, Function<Map<String, Object>, T> factory) {
        List<T> out = new ArrayList<>();
        for (Object item : list(value)) {
            if (item instanceof Map) {
                out.add(factory.apply((Map<String, Object>) item));
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static List<String> stringList(Object value) {
        List<String> out = new ArrayList<>();
        for (Object item : list(value)) {
            if (item instanceof String s) {
                out.add(s);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
