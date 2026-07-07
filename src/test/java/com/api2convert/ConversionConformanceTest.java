package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.api2convert.exception.AuthenticationException;
import com.api2convert.exception.ConversionFailedException;
import com.api2convert.exception.ValidationException;
import com.api2convert.http.Config;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Live conformance suite — the canonical, cross-SDK set of scenarios that exercises the real
 * API2Convert API end to end. Every scenario is written to read like a usage example, so this file
 * doubles as an executable tour of the SDK: build a client, convert, discover, drive the job
 * lifecycle by hand, and handle the typed errors.
 *
 * <p>Because these hit the real API and consume quota, the whole class auto-skips unless
 * {@code API2CONVERT_API_KEY} is set (via {@link EnabledIfEnvironmentVariable}) — so it is safe in
 * the default suite — and it is tagged {@code live} so {@code mvn verify} can exclude it explicitly.
 *
 * <p>Run it against a host with:
 * <pre>{@code
 * API2CONVERT_API_KEY=... mvn verify -DexcludedGroups=
 * # or target a beta host:
 * API2CONVERT_API_KEY=... API2CONVERT_BASE_URL=https://api.web3.beta.api2convert.com/v2 \
 *     mvn verify -DexcludedGroups=
 * }</pre>
 *
 * <p>The seven scenarios mirror the shared spec implemented by every api2convert SDK (php, python,
 * java, go, nodejs, dotnet, ruby, rust):
 *
 * <ol>
 *   <li>{@link #convertRemoteUrlToPng()} — one-call convert of a URL</li>
 *   <li>{@link #uploadLocalFileAndConvert()} — multipart upload of a local file</li>
 *   <li>{@link #convertWithOptions()} — apply conversion options</li>
 *   <li>{@link #discoverConversionCatalog()} — options/catalog discovery</li>
 *   <li>{@link #manualJobLifecycleAndInspection()} — create &rarr; input &rarr; start &rarr; wait</li>
 *   <li>{@link #invalidTargetIsTypedError()} — validation error handling</li>
 *   <li>{@link #authenticationErrorLeaksNoSecret()} — auth error, no key leak</li>
 * </ol>
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "API2CONVERT_API_KEY", matches = ".+")
class ConversionConformanceTest {

    /** A small, stable public image used as a remote input everywhere in the suite. */
    private static final String REMOTE_JPG =
            "https://example-files.online-convert.com/raster%20image/jpg/example_small.jpg";

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

    // 1. One-call convert of a remote URL --------------------------------------------------------
    //
    // The simplest usage: hand convert() a URL and a target format. The SDK creates a
    // server-side-fetch job, polls it to completion, and hands back a result you can save to disk.
    @Test
    @DisplayName("1. convert a remote URL to PNG")
    void convertRemoteUrlToPng() throws IOException {
        ConversionResult result = client().convert(REMOTE_JPG, "png");
        assertTrue(result.job().isCompleted(), "job should complete");

        // save() to a directory keeps the server-provided filename; it returns the written path.
        Path dir = Files.createTempDirectory("a2c-live-remote");
        String saved = result.save(dir.toString());
        assertTrue(Files.size(Path.of(saved)) > 0, "output should be non-empty");
    }

    // 2. Upload and convert a local file ---------------------------------------------------------
    //
    // For a local path (or a Path/byte[]/InputStream), the SDK stages the job, streams the file to
    // the per-job upload server (authenticated with the job's own token, never your account key),
    // starts it, polls, and downloads.
    @Test
    @DisplayName("2. upload a local file and convert")
    void uploadLocalFileAndConvert() throws IOException {
        Path src = Files.createTempDirectory("a2c-live-upload").resolve("pixel.png");
        Files.write(src, ONE_PX_PNG);

        ConversionResult result = client().convert(src.toString(), "jpg");
        assertTrue(result.job().isCompleted(), "uploaded job should complete");

        byte[] bytes = result.contents();
        assertTrue(bytes.length > 0, "converted output should be non-empty");
        // A JPEG starts with the SOI marker 0xFF 0xD8.
        assertEquals((byte) 0xFF, bytes[0], "output should be a JPEG (SOI byte 0)");
        assertEquals((byte) 0xD8, bytes[1], "output should be a JPEG (SOI byte 1)");
    }

    // 3. Apply conversion options ----------------------------------------------------------------
    //
    // Target-specific options are their own map, kept separate from the SDK's controls so an option
    // key can never collide with an SDK argument. Discover valid keys with client.options() (see the
    // next scenario); here we re-encode at a lower JPEG quality.
    @Test
    @DisplayName("3. convert with conversion options")
    void convertWithOptions() {
        // Add e.g. "width", 64, "height", 64 to the options map to resize.
        ConversionResult result = client().convert(REMOTE_JPG, "jpg", Map.of("quality", 50));
        assertTrue(result.job().isCompleted(), "job should complete");

        assertTrue(result.contents().length > 0, "converted output should be non-empty");
    }

    // 4. Discover the conversion catalog ---------------------------------------------------------
    //
    // conversions().list() and options() describe what the API can do — which targets exist and which
    // options each accepts. Neither consumes conversion quota, so they are cheap to call up front.
    @Test
    @DisplayName("4. discover the conversion catalog")
    void discoverConversionCatalog() {
        Api2Convert client = client();

        // Which conversions target "jpg"? (filter: category=null, target="jpg", page=1)
        List<Map<String, Object>> conversions = client.conversions().list(null, "jpg", 1);
        assertFalse(conversions.isEmpty(), "the catalog should list at least one conversion to jpg");

        // The option schema for a target (type / enum / default / range per option). The call
        // succeeding is the assertion; "image" disambiguates the "png" target.
        Map<String, Object> options = client.options("png", "image");
        assertTrue(options != null, "fetching the option schema should succeed");
    }

    // 5. Drive the full job lifecycle by hand ----------------------------------------------------
    //
    // convert() is built from these primitives. Driving them yourself unlocks compound/merge jobs,
    // custom inputs, and step-by-step inspection: create a staged job, attach an input, start it,
    // wait for completion, then inspect the job's status and output metadata.
    @Test
    @DisplayName("5. manual job lifecycle and inspection")
    void manualJobLifecycleAndInspection() {
        var jobs = client().jobs();

        // Stage a job (process=false) so we can attach inputs before starting.
        Job job = jobs.create(Map.of(
                "process", false,
                "conversion", List.of(Map.of("target", "png"))));
        assertFalse(job.id().isEmpty(), "a created job has an id");

        // Attach a remote input, then start processing.
        jobs.addInput(job.id(), Map.of("type", "remote", "source", REMOTE_JPG));
        jobs.start(job.id());

        // Poll to a terminal status (throwOnFailure=true turns a failed job into an exception).
        Job finished = jobs.await(job.id(), null, true);
        assertTrue(finished.isCompleted(), "job should complete");

        // Inspect the outputs — both from the finished job and via the outputs() API.
        assertFalse(finished.output().isEmpty(), "job should have an output");
        List<OutputFile> outputs = jobs.outputs(job.id());
        assertEquals(finished.output().size(), outputs.size(),
                "outputs() should match the job's output list");

        OutputFile first = finished.output().get(0);
        assertFalse(first.uri().isEmpty(), "output has a download URI");
    }

    // 6. Validation error on an unknown target ---------------------------------------------------
    //
    // The API rejects an unknown target — synchronously at create time (a ValidationException), or,
    // depending on the target, as a failed job (a ConversionFailedException). Both are typed errors
    // you can catch.
    @Test
    @DisplayName("6. an invalid target is a typed error")
    void invalidTargetIsTypedError() {
        try {
            client().convert(REMOTE_JPG, "this-is-not-a-real-target");
            fail("an unknown target should fail");
        } catch (ValidationException | ConversionFailedException expected) {
            // A typed error, exactly as documented.
        }
    }

    // 7. Authentication error, with no secret leak -----------------------------------------------
    //
    // A bad key produces a typed AuthenticationException carrying the HTTP status. Crucially, the SDK
    // never puts a credential into an error message — we assert the bogus key does not appear in the
    // rendered error.
    @Test
    @DisplayName("7. an authentication error never leaks the key")
    void authenticationErrorLeaksNoSecret() {
        // Keep this test in the live suite (gated on the real key like the rest) so it exercises the
        // real auth path — but authenticate a SECOND client with a deliberately bogus key.
        String bogusKey = "a2c-invalid-key-for-testing";
        Api2Convert bogusClient = new Api2Convert(bogusKey, config());

        AuthenticationException error =
                assertThrows(AuthenticationException.class, () -> bogusClient.jobs().list());

        int status = error.getStatusCode();
        assertTrue(status == 401 || status == 403, "expected HTTP 401/403, got " + status);

        // The rendered error must not leak the credential (check both the message and toString()).
        String rendered = error.getMessage() + " " + error;
        assertFalse(rendered.contains(bogusKey), "the error message must not leak the API key");
    }
}
