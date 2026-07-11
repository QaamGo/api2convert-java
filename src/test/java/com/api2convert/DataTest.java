package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.support.Data;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataTest {

    @Test
    void nullableIntRejectsBooleanAndCoercesStrings() {
        assertNull(Data.nullableInt(true), "a boolean is not numeric (mirrors PHP is_numeric)");
        assertNull(Data.nullableInt(null));
        assertNull(Data.nullableInt("abc"));
        assertEquals(5, Data.nullableInt(5));
        assertEquals(5, Data.nullableInt("5"));
        assertEquals(5, Data.nullableInt(5.9));
        // Out of int range -> null (absence), never a truncated/wrapped value.
        assertNull(Data.nullableInt(5_000_000_000L));
    }

    @Test
    void nullableLongHandlesLargeValues() {
        assertEquals(5_000_000_000L, Data.nullableLong(5_000_000_000L));
        assertEquals(5_000_000_000L, Data.nullableLong("5000000000"));
        assertEquals(Long.MAX_VALUE, Data.nullableLong(Long.MAX_VALUE));
        assertNull(Data.nullableLong(true));

        // Out-of-long-range values hydrate to null, never a wrapped (BigInteger) or saturated (double)
        // garbage value.
        assertNull(Data.nullableLong(new java.math.BigInteger("10000000000000000000")));
        assertNull(Data.nullableLong(1e19));
        assertNull(Data.nullableLong(-1e19));
        assertNull(Data.nullableLong("1e19"));
        assertNull(Data.nullableLong(Double.NaN));
        assertNull(Data.nullableLong(Double.POSITIVE_INFINITY));
    }

    @Test
    void stringOnlyAcceptsGenuineStrings() {
        assertEquals("", Data.string(123));
        assertEquals("x", Data.string("x"));
        assertNull(Data.nullableString(123));
    }

    @Test
    void listReducesAMapToItsValues() {
        assertTrue(Data.list(null).isEmpty());
        assertEquals(List.of(1, 2), Data.list(List.of(1, 2)));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(List.of(1, 2), Data.list(map));
    }

    @Test
    void objectToleratesNonObjects() {
        assertTrue(Data.object("nope").isEmpty());
        assertTrue(Data.object(null).isEmpty());
        assertEquals(Map.of("a", 1), Data.object(Map.of("a", 1)));
    }
}
