package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.api2convert.http.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

class ClientConfigTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "API2CONVERT_API_KEY", matches = ".+")
    void missingKeyThrowsWhenNoEnvVar() {
        assertThrows(IllegalArgumentException.class, () -> new Api2Convert(""));
        assertThrows(IllegalArgumentException.class, () -> new Api2Convert(null));
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
