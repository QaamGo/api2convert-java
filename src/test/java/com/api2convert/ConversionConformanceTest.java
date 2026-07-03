package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.ValidationException;
import com.api2convert.http.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * End-to-end conformance against the real API. Auto-skipped unless API2CONVERT_API_KEY is set, so it
 * is safe in the default suite; tagged {@code live} so it can also be excluded explicitly.
 *
 * <p>Run against a host with:
 * {@code API2CONVERT_API_KEY=... API2CONVERT_BASE_URL=https://api.web3.beta.api2convert.com/v2 mvn verify -DexcludedGroups=}
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "API2CONVERT_API_KEY", matches = ".+")
class ConversionConformanceTest {

    private static final String EXAMPLE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";

    private Api2Convert client() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        Config config = base != null && !base.isEmpty()
                ? Config.builder().baseUrl(base).build()
                : Config.defaults();
        return new Api2Convert(System.getenv("API2CONVERT_API_KEY"), config);
    }

    @Test
    void convertsRemoteImageToPng() throws IOException {
        ConversionResult result = client().convert(EXAMPLE_JPG, "png");

        assertTrue(result.job().isCompleted());
        Path target = Files.createTempFile("a2c-live", ".png");
        result.save(target.toString());
        assertTrue(Files.size(target) > 0);
        Files.deleteIfExists(target);
    }

    @Test
    void invalidTargetRaisesValidationError() {
        // The real API rejects an unknown target synchronously at job creation (HTTP 400 ->
        // ValidationException), not as an async failed job. The failed/canceled-job ->
        // ConversionFailedException path is covered by the unit suite (WaitTest, PollingGuardsTest).
        assertThrows(ValidationException.class, () -> client().convert(EXAMPLE_JPG, "this-is-not-a-real-target"));
    }
}
