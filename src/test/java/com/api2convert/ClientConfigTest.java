package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.exception.ConfigurationException;
import com.api2convert.http.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

class ClientConfigTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "API2CONVERT_API_KEY", matches = ".+")
    void missingKeyThrowsWhenNoEnvVar() {
        // A missing key must surface as a typed SDK exception (inside the Api2ConvertException
        // hierarchy), not a bare java.lang.IllegalArgumentException, so callers can catch it uniformly.
        ConfigurationException empty = assertThrows(ConfigurationException.class, () -> new Api2Convert(""));
        ConfigurationException nul = assertThrows(ConfigurationException.class, () -> new Api2Convert(null));
        assertTrue(empty instanceof Api2ConvertException);
        assertTrue(nul instanceof Api2ConvertException);
    }

    @Test
    void explicitKeyIsAccepted() {
        assertDoesNotThrow(() -> new Api2Convert("a-key"));
    }

    @Test
    void baseUrlTrailingSlashIsTrimmed() {
        assertEquals("https://api.example.com/v2", Config.builder().baseUrl("https://api.example.com/v2/").build().baseUrl());
    }

    @Test
    void defaultsAreSane() {
        Config config = Config.defaults();
        assertEquals(Config.DEFAULT_BASE_URL, config.baseUrl());
        assertEquals(30, config.timeout());
        assertEquals(2, config.maxRetries());
    }
}
