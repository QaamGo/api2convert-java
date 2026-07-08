package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.api2convert.exception.AuthenticationException;
import com.api2convert.exception.ConversionFailedException;
import com.api2convert.exception.ValidationException;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import com.api2convert.model.Preset;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Live conformance suite — the canonical, cross-SDK set of documented examples that exercises the
 * real API2Convert API end to end. Each test mirrors one runnable file in {@code examples/} and one
 * guide on api2convert.com, so this file doubles as an executable tour of the SDK.
 *
 * <p>Because these hit the real API and consume quota, the whole class auto-skips unless
 * {@code API2CONVERT_API_KEY} is set (via {@link EnabledIfEnvironmentVariable}) — so it is safe in
 * the default suite — and it is tagged {@code live} so {@code mvn verify} excludes it by default.
 *
 * <p>Run it against a host with:
 * <pre>{@code
 * API2CONVERT_API_KEY=... mvn test -Dgroups=live -DexcludedGroups=
 * # or target a beta host:
 * API2CONVERT_API_KEY=... API2CONVERT_BASE_URL=https://api.web3.beta.api2convert.com/v2 \
 *     mvn test -Dgroups=live -DexcludedGroups=
 * }</pre>
 *
 * <p>The 20 documented examples plus two negative scenarios (an invalid target is a typed validation
 * error; a bad key is a typed auth error that never leaks the credential) make up the shared spec
 * implemented by every api2convert SDK.
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "API2CONVERT_API_KEY", matches = ".+")
class ConversionConformanceTest {

    // Public example fixtures (the online-convert.com example files).
    private static final String REMOTE_PDF =
            "https://example-files.online-convert.com/document/pdf/example.pdf";
    private static final String REMOTE_PNG =
            "https://example-files.online-convert.com/raster%20image/png/example.png";
    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example.jpg";
    private static final String REMOTE_JPG_SMALL =
            "https://example-files.online-convert.com/raster%20image/jpg/example_small.jpg";
    private static final String REMOTE_WAV =
            "https://example-files.online-convert.com/audio/wav/example.wav";
    private static final String REMOTE_DOCX =
            "https://example-files.online-convert.com/document/docx/example.docx";
    private static final String REMOTE_ZIP =
            "https://example-files.online-convert.com/archive/zip/example.zip";

    /**
     * A minimal valid 1&times;1 PNG, written to disk to exercise the real multipart upload handshake
     * (remote-URL inputs skip upload entirely).
     */
    private static final byte[] ONE_PX_PNG = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53,
        (byte) 0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00,
        0x00, 0x00, 0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
        0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82,
    };

    /**
     * Build a live client from the environment. The API key comes only from {@code API2CONVERT_API_KEY}
     * (the class is disabled without it), and {@code API2CONVERT_BASE_URL} optionally retargets the
     * host, so the same suite can run against prod or a beta environment.
     */
    private Api2Convert client() {
        return new Api2Convert(System.getenv("API2CONVERT_API_KEY"), config());
    }

    private Config config() {
        String base = System.getenv("API2CONVERT_BASE_URL");
        return base != null && !base.isEmpty()
                ? Config.builder().baseUrl(base).build()
                : Config.defaults();
    }

    // 1. Quickstart: convert a remote JPG to PNG, look the job back up, download the output. --------
    @Test
    @DisplayName("01. quickstart — convert remote jpg to png, get job, download")
    void quickstart() throws IOException {
        Api2Convert client = client();
        ConversionResult result = client.convert(REMOTE_JPG, "png");
        assertTrue(result.job().isCompleted(), "job should complete");

        Job fetched = client.jobs().get(result.job().id());
        assertTrue(fetched.isCompleted(), "the fetched job should be completed");

        Path dir = Files.createTempDirectory("a2c-quickstart");
        String saved = result.save(dir.toString());
        assertTrue(Files.size(Path.of(saved)) > 0, "output should be non-empty");
    }

    // 2. Convert Files: list the catalog (all, then filtered), then convert. -----------------------
    @Test
    @DisplayName("02. convert-files — discover the catalog then convert")
    void convertFiles() {
        Api2Convert client = client();

        List<Map<String, Object>> all = client.conversions().list();
        assertFalse(all.isEmpty(), "the catalog should list at least one conversion");

        List<Map<String, Object>> toPng = client.conversions().list(null, "png", 1);
        assertFalse(toPng.isEmpty(), "the catalog should list at least one conversion to png");

        ConversionResult result = client.convert(REMOTE_JPG, "png");
        assertTrue(result.job().isCompleted(), "job should complete");
    }

    // 3. Uploading Files: one-call upload + convert of a local file. -------------------------------
    @Test
    @DisplayName("03. uploading-files — upload a local file and convert")
    void uploadingFiles() throws IOException {
        Path src = Files.createTempDirectory("a2c-upload").resolve("pixel.png");
        Files.write(src, ONE_PX_PNG);

        ConversionResult result = client().convert(src.toString(), "png");
        assertTrue(result.job().isCompleted(), "uploaded job should complete");
        assertTrue(result.contents().length > 0, "converted output should be non-empty");
    }

    // 4. Job Lifecycle: create (staged) -> add remote input -> start -> wait -> outputs. -----------
    @Test
    @DisplayName("04. job-lifecycle — manual create/input/start/wait")
    void jobLifecycle() {
        var jobs = client().jobs();

        Job job = jobs.create(Map.of(
                "process", false,
                "conversion", List.of(Map.of("category", "image", "target", "png"))));
        assertFalse(job.id().isEmpty(), "a created job has an id");

        jobs.addInput(job.id(), Map.of("type", "remote", "source", REMOTE_JPG));
        jobs.start(job.id());

        Job done = jobs.await(job.id(), null, true);
        assertTrue(done.isCompleted(), "job should complete");
        assertFalse(done.output().isEmpty(), "job should have outputs");
        assertFalse(jobs.outputs(job.id()).isEmpty(), "outputs() should be non-empty");
    }

    // 5. Add Watermark: two remote inputs stamped into a PDF. --------------------------------------
    @Test
    @DisplayName("05. add-watermark — stamp an image onto a PDF")
    void addWatermark() {
        var jobs = client().jobs();
        Job job = jobs.create(Map.of(
                "process", true,
                "input", List.of(
                        Map.of("type", "remote", "source", REMOTE_PDF),
                        Map.of("type", "remote", "source", REMOTE_PNG)),
                "conversion", List.of(Map.of(
                        "category", "document",
                        "target", "pdf",
                        "options", Map.of("stamp", true, "alignment", "center")))));

        Job done = jobs.await(job.id());
        assertTrue(done.isCompleted(), "job should complete");
        assertFalse(done.output().isEmpty(), "job should have outputs");
    }

    // 6. Create Thumbnails: PDF -> thumbnail (operation). ------------------------------------------
    @Test
    @DisplayName("06. create-thumbnails — first-page thumbnail of a PDF")
    void createThumbnails() {
        ConversionResult result = client().convert(REMOTE_PDF, "thumbnail",
                Map.of("thumbnail_target", "png", "width", 300, "pages", "first", "dpi", 150),
                new ConvertOptions().category("operation"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "thumbnail should be non-empty");
    }

    // 7. Compress Files: JPG -> compress (operation). ---------------------------------------------
    @Test
    @DisplayName("07. compress-files — compress a JPG")
    void compressFiles() {
        ConversionResult result = client().convert(REMOTE_JPG, "compress",
                Map.of("compression_level", "high"),
                new ConvertOptions().category("operation"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "compressed output should be non-empty");
    }

    // 8. Create Archives: two remote inputs -> zip. -----------------------------------------------
    @Test
    @DisplayName("08. create-archives — bundle two files into a ZIP")
    void createArchives() {
        var jobs = client().jobs();
        Job job = jobs.create(Map.of(
                "process", true,
                "input", List.of(
                        Map.of("type", "remote", "source", REMOTE_PDF),
                        Map.of("type", "remote", "source", REMOTE_PNG)),
                "conversion", List.of(Map.of("category", "archive", "target", "zip"))));

        Job done = jobs.await(job.id());
        assertTrue(done.isCompleted(), "job should complete");
        assertFalse(done.output().isEmpty(), "job should have outputs");
    }

    // 9. Create Hashes: ZIP -> sha256 (hash). -----------------------------------------------------
    @Test
    @DisplayName("09. create-hashes — SHA-256 of a file")
    void createHashes() {
        ConversionResult result = client().convert(REMOTE_ZIP, "sha256", null,
                new ConvertOptions().category("hash"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "the hash output should be non-empty");
    }

    // 10. Extract Assets: DOCX -> extract-assets (operation). -------------------------------------
    @Test
    @DisplayName("10. extract-assets — extract embedded assets from a DOCX")
    void extractAssets() {
        ConversionResult result = client().convert(REMOTE_DOCX, "extract-assets", null,
                new ConvertOptions().category("operation"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertFalse(result.outputs().isEmpty(), "job should produce outputs");
    }

    // 11. File Analysis: JPG -> json (metadata). --------------------------------------------------
    @Test
    @DisplayName("11. file-analysis — extract metadata as JSON")
    void fileAnalysis() {
        ConversionResult result = client().convert(REMOTE_JPG, "json", null,
                new ConvertOptions().category("metadata"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "the metadata JSON should be non-empty");
    }

    // 12. Compare Files: two JPGs -> compare-image (operation). -----------------------------------
    @Test
    @DisplayName("12. compare-files — SSIM diff of two images")
    void compareFiles() {
        var jobs = client().jobs();
        Job job = jobs.create(Map.of(
                "process", true,
                "input", List.of(
                        Map.of("type", "remote", "source", REMOTE_JPG_SMALL),
                        Map.of("type", "remote", "source", REMOTE_JPG)),
                "conversion", List.of(Map.of(
                        "category", "operation",
                        "target", "compare-image",
                        "options", Map.of("method", "ssim", "threshold", 5, "diff_color", "red")))));

        Job done = jobs.await(job.id());
        assertTrue(done.isCompleted(), "job should complete");
    }

    // 13. Capture Website: screenshot engine -> png. ---------------------------------------------
    @Test
    @DisplayName("13. capture-website — screenshot a URL to PNG")
    void captureWebsite() {
        var jobs = client().jobs();
        Job job = jobs.create(Map.of(
                "process", true,
                "input", List.of(Map.of(
                        "type", "remote",
                        "source", "https://www.online-convert.com",
                        "engine", "screenshot",
                        "options", Map.of(
                                "screen_width", 1280,
                                "screen_height", 1024,
                                "device_scale_factor", 1))),
                "conversion", List.of(Map.of("category", "image", "target", "png"))));

        Job done = jobs.await(job.id());
        assertTrue(done.isCompleted(), "job should complete");
        assertFalse(done.output().isEmpty(), "job should produce a screenshot");
    }

    // 14. Audio Operations: WAV -> aac (audio). ---------------------------------------------------
    @Test
    @DisplayName("14. audio-operations — transcode WAV to AAC")
    void audioOperations() {
        ConversionResult result = client().convert(REMOTE_WAV, "aac",
                Map.of("audio_codec", "aac", "audio_bitrate", 192, "channels", "stereo", "frequency", 44100),
                new ConvertOptions().category("audio"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "the audio output should be non-empty");
    }

    // 15. Image Operations: JPG -> resize-image (operation). --------------------------------------
    @Test
    @DisplayName("15. image-operations — resize an image")
    void imageOperations() {
        ConversionResult result = client().convert(REMOTE_JPG, "resize-image",
                Map.of("width", 800, "height", 600, "resize_by", "px", "resize_handling", "keep_aspect_ratio_crop"),
                new ConvertOptions().category("operation"));
        assertTrue(result.job().isCompleted(), "job should complete");
        assertTrue(result.contents().length > 0, "the resized image should be non-empty");
    }

    // 16. Webhooks: start an async conversion with a callback (do not wait for the webhook). -------
    @Test
    @DisplayName("16. webhooks — start an async job with a callback")
    void webhooks() {
        Job job = client().convertAsync(REMOTE_DOCX, "pdf", null,
                new AsyncOptions().category("document").callback("https://your-app.example.com/api2convert/webhook"));
        assertNotNull(job, "convertAsync should return a job");
        assertFalse(job.id().isEmpty(), "the started job should have an id");
    }

    // 17. Presets: list saved presets (may be empty). --------------------------------------------
    @Test
    @DisplayName("17. presets — list saved presets")
    void presets() {
        List<Preset> presets = client().presets().list("video", "mp4", null);
        assertNotNull(presets, "presets().list should return a list");
    }

    // 18. Statistics: usage for a recent month. --------------------------------------------------
    @Test
    @DisplayName("18. statistics — monthly usage")
    void statistics() {
        String month = YearMonth.now().toString();
        assertDoesNotThrow(() -> client().stats().month(month), "stats().month should not error");
    }

    // 19. Rate Limits / Contracts: read the account's contracts. ---------------------------------
    @Test
    @DisplayName("19. rate-limits — read account contracts")
    void rateLimits() {
        assertDoesNotThrow(() -> client().contracts().get(), "contracts().get should not error");
    }

    // 20. Authentication: an authenticated call succeeds. ----------------------------------------
    @Test
    @DisplayName("20. authentication — an authenticated call succeeds")
    void authentication() {
        List<Job> jobs = client().jobs().list();
        assertNotNull(jobs, "jobs().list should return a list");
    }

    // --- negative scenarios ---------------------------------------------------------------------

    // An unknown target is rejected — synchronously at create time (a ValidationException), or,
    // depending on the target, as a failed job (a ConversionFailedException). Both are typed.
    @Test
    @DisplayName("neg. an invalid target is a typed error")
    void invalidTargetIsTypedError() {
        try {
            client().convert(REMOTE_JPG, "this-is-not-a-real-target");
            fail("an unknown target should fail");
        } catch (ValidationException | ConversionFailedException expected) {
            // A typed error, exactly as documented.
        }
    }

    // A bad key produces a typed AuthenticationException carrying the HTTP status. The SDK never puts
    // a credential into an error message — assert the bogus key does not appear in the rendered error.
    @Test
    @DisplayName("neg. an authentication error never leaks the key")
    void authenticationErrorLeaksNoSecret() {
        String bogusKey = "a2c-invalid-key-for-testing";
        Api2Convert bogusClient = new Api2Convert(bogusKey, config());

        AuthenticationException error =
                assertThrows(AuthenticationException.class, () -> bogusClient.jobs().list());

        int status = error.getStatusCode();
        assertTrue(status == 401 || status == 403, "expected HTTP 401/403, got " + status);

        String rendered = error.getMessage() + " " + error;
        assertFalse(rendered.contains(bogusKey), "the error message must not leak the API key");
    }
}
