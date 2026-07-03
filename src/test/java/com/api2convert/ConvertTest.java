package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.model.OutputFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConvertTest extends A2CTestCase {

    @Test
    void convertRemoteUrlCreatesStartedJobPollsAndDownloads() throws IOException {
        http.addJson(201, """
                {"id":"job-1","status":{"code":"incomplete","info":"Queued"}}
                """);
        http.addJson(200, """
                {"id":"job-1","status":{"code":"completed"},
                 "output":[{"id":"out-1","uri":"https://dl.example.com/result.png",
                            "filename":"result.png","content_type":"image/png"}]}
                """);
        http.addRaw(200, "PNGDATA".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = client().convert("https://example.com/photo.jpg", "png");

        // 1) create job, started immediately, with the remote input inline
        Recorded create = requestAt(0);
        assertEquals("POST", create.method());
        assertTrue(create.uri().endsWith("/jobs"));
        assertEquals("test-key", create.header("X-Oc-Api-Key"));
        assertFalse(create.followRedirects());
        Map<String, Object> body = create.bodyJson();
        assertEquals(Boolean.TRUE, body.get("process"));
        assertEquals("png", conversion0(body).get("target"));
        Map<?, ?> input0 = (Map<?, ?>) ((List<?>) body.get("input")).get(0);
        assertEquals("remote", input0.get("type"));
        assertEquals("https://example.com/photo.jpg", input0.get("source"));

        // 2) poll
        assertEquals("GET", requestAt(1).method());
        assertTrue(requestAt(1).uri().endsWith("/jobs/job-1"));

        // 3) save() downloads the output to disk
        assertEquals("https://dl.example.com/result.png", result.url());
        Path target = Files.createTempDirectory("a2c").resolve("out.png");
        String written = result.save(target.toString());
        assertEquals(target.toString(), written);
        assertArrayEquals("PNGDATA".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(Path.of(written)));
        assertEquals("https://dl.example.com/result.png", requestAt(2).uri());
        assertTrue(requestAt(2).followRedirects(), "download opts into redirects (self-contained URL, no key)");
        Files.deleteIfExists(target);
    }

    @Test
    void convertLocalFileStagesUploadsThenStarts() throws IOException {
        Path source = Files.createTempFile("a2c-src", ".txt");
        Files.writeString(source, "hello world");

        http.addJson(201, """
                {"id":"job-9","token":"tok-abc","server":"https://www2.api2convert.com/v2",
                 "status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"in-1","type":"upload","filename":"src.txt","status":"downloaded"}
                """);
        http.addJson(200, """
                {"id":"job-9","status":{"code":"processing"}}
                """);
        http.addJson(200, """
                {"id":"job-9","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/out.pdf","filename":"out.pdf"}]}
                """);

        ConversionResult result = client().convert(source.toString(), "pdf");

        // 1) staged create
        assertEquals(Boolean.FALSE, requestAt(0).bodyJson().get("process"));

        // 2) upload to the per-job server, authenticated with the job token (NOT the api key)
        Recorded upload = requestAt(1);
        assertEquals("POST", upload.method());
        assertEquals("https://www2.api2convert.com/v2/upload-file/job-9", upload.uri());
        assertEquals("tok-abc", upload.header("X-Oc-Token"));
        assertEquals("", upload.header("X-Oc-Api-Key"));
        assertFalse(upload.followRedirects());
        assertTrue(upload.header("Content-Type").contains("multipart/form-data"));
        assertTrue(upload.bodyString().contains("name=\"file\""));

        // 3) start
        Recorded start = requestAt(2);
        assertEquals("PATCH", start.method());
        assertEquals(Boolean.TRUE, start.bodyJson().get("process"));

        assertEquals("out.pdf", result.output().filename());
        Files.deleteIfExists(source);
    }

    @Test
    void convertAsyncReturnsImmediatelyWithCallback() {
        http.addJson(201, """
                {"id":"job-async","status":{"code":"incomplete"}}
                """);

        var job = client().convertAsync("https://example.com/a.mov", "mp4", null,
                new AsyncOptions().callback("https://app.example.com/hook"));

        assertEquals("job-async", job.id());
        assertEquals(1, http.requests.size());
        Map<String, Object> body = requestAt(0).bodyJson();
        assertEquals("https://app.example.com/hook", body.get("callback"));
        assertEquals(Boolean.TRUE, body.get("notify_status"));
    }

    @Test
    void convertForwardsOptionsAsConversionOptions() {
        http.addJson(201, """
                {"id":"j","status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"j","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/out.jpg","filename":"out.jpg"}]}
                """);

        client().convert("https://example.com/photo.png", "jpg", Map.of("quality", 85, "width", 1280));

        Map<?, ?> conversion = conversion0(requestAt(0).bodyJson());
        assertEquals("jpg", conversion.get("target"));
        assertEquals(Map.of("quality", 85, "width", 1280), conversion.get("options"));
    }

    @Test
    void contentsDownloadsBody() {
        http.addJson(201, """
                {"id":"j","status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"j","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/out.png"}]}
                """);
        http.addRaw(200, "RAWBYTES".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = client().convert("https://example.com/photo.jpg", "png");

        assertEquals("RAWBYTES", new String(result.contents(), StandardCharsets.UTF_8));
        assertEquals("https://dl.example.com/out.png", requestAt(2).uri());
    }

    @Test
    void convertWithDownloadPasswordSetsItAndSendsHeaderTransparently() {
        http.addJson(201, """
                {"id":"j","status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"j","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/secret.pdf","filename":"secret.pdf"}]}
                """);
        http.addRaw(200, "SECRET".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = client().convert("https://example.com/a.docx", "pdf", null,
                new ConvertOptions().downloadPassword("hunter2"));

        assertEquals(List.of("hunter2"), requestAt(0).bodyJson().get("download_passwords"));
        assertEquals("SECRET", new String(result.contents(), StandardCharsets.UTF_8));
        assertEquals("hunter2", requestAt(2).header("X-Oc-Download-Password"));
    }

    @Test
    void explicitDownloadPasswordOverridesRememberedOne() {
        http.addJson(201, """
                {"id":"j","status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"j","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/secret.pdf"}]}
                """);
        http.addRaw(200, "SECRET".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = client().convert("https://example.com/a.docx", "pdf", null,
                new ConvertOptions().downloadPassword("hunter2"));
        result.contents("override-pw");

        assertEquals("override-pw", requestAt(2).header("X-Oc-Download-Password"));
    }

    @Test
    void downloadHelperCarriesDownloadPassword() {
        http.addRaw(200, "BYTES".getBytes(StandardCharsets.UTF_8));

        OutputFile output = OutputFile.of("o", "https://dl.example.com/secret.pdf", "secret.pdf");
        byte[] bytes = client().download(output, "hunter2").contents();

        assertEquals("BYTES", new String(bytes, StandardCharsets.UTF_8));
        assertEquals("hunter2", requestAt(0).header("X-Oc-Download-Password"));
    }

    @Test
    void convertAsyncSetsDownloadPasswordOnCreate() {
        http.addJson(201, """
                {"id":"job-async","status":{"code":"incomplete"}}
                """);

        client().convertAsync("https://example.com/a.mov", "mp4", null,
                new AsyncOptions().downloadPassword("hunter2"));

        assertEquals(List.of("hunter2"), requestAt(0).bodyJson().get("download_passwords"));
    }

    @Test
    void convertWithoutDownloadPasswordSendsNoHeaderOrField() {
        http.addJson(201, """
                {"id":"j","status":{"code":"incomplete"}}
                """);
        http.addJson(200, """
                {"id":"j","status":{"code":"completed"},
                 "output":[{"id":"o","uri":"https://dl.example.com/out.png"}]}
                """);
        http.addRaw(200, "BYTES".getBytes(StandardCharsets.UTF_8));

        ConversionResult result = client().convert("https://example.com/photo.jpg", "png");
        result.contents();

        assertFalse(requestAt(0).bodyJson().containsKey("download_passwords"));
        assertEquals("", requestAt(2).header("X-Oc-Download-Password"));
    }

    @Test
    void optionsDiscoveryQueriesByTargetOnly() {
        http.addJson(200, """
                [{"id":"image-to-jpg","category":"image","target":"jpg",
                  "options":{"quality":{"type":"integer"}}}]
                """);

        Map<String, Object> options = client().options("jpg");

        assertTrue(options.containsKey("quality"));
        String uri = requestAt(0).uri();
        assertTrue(uri.contains("target=jpg"));
        assertFalse(uri.contains("category="));
    }

    private static Map<?, ?> conversion0(Map<String, Object> body) {
        return (Map<?, ?>) ((List<?>) body.get("conversion")).get(0);
    }
}
