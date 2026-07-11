package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.AuthenticationException;
import com.api2convert.exception.NetworkException;
import com.api2convert.http.Config;
import com.api2convert.model.InputFile;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Security guardrails. The redirect tests use real loopback HTTP servers to prove the JDK-backed
 * transport's per-request redirect policy: the account key never follows a cross-host redirect on an
 * authenticated request (guard #7), while the self-contained download path does follow storage
 * redirects.
 */
class SecurityTest extends A2CTestCase {

    @Test
    void secretNeverAppearsInExceptionMessage() {
        String secretKey = "sk_live_super_secret_value_123";
        http.addJson(401, "{\"message\":\"Invalid API key.\"}");

        AuthenticationException e = assertThrows(AuthenticationException.class, () ->
                new Api2Convert(secretKey, Config.builder().maxRetries(0).build(), http, s -> slept.add(s))
                        .jobs().get("job-x"));

        assertFalse(e.getMessage().contains(secretKey), "the API key must never leak into an exception message");
        // ...but it WAS sent as the auth header, so the request was genuinely authenticated.
        assertEquals(secretKey, requestAt(0).header("X-Oc-Api-Key"));
    }

    @Test
    void apiKeyIsNotFollowedAcrossARedirectOnTheAuthenticatedPath() throws IOException {
        AtomicInteger evilHits = new AtomicInteger();
        HttpServer evil = start(exchange -> {
            evilHits.incrementAndGet();
            respond(exchange, 200, "grabbed:" + exchange.getRequestHeaders().getFirst("X-Oc-Api-Key"));
        });
        HttpServer api = start(exchange -> {
            // An API host (or a compromised intermediary) that 302s to another host must not cause
            // the account key to be forwarded there.
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + evil.getAddress().getPort() + "/steal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        try {
            Api2Convert client = new Api2Convert("secret-key", Config.builder()
                    .baseUrl("http://127.0.0.1:" + api.getAddress().getPort() + "/v2")
                    .maxRetries(0)
                    .build());
            // An authenticated 3xx is surfaced as a typed error, not silently empty.
            assertThrows(NetworkException.class, () -> client.jobs().get("j"));

            assertEquals(0, evilHits.get(), "the account key must never be forwarded to the redirect target");
        } finally {
            api.stop(0);
            evil.stop(0);
        }
    }

    @Test
    void downloadFollowsStorageRedirects() throws IOException {
        HttpServer storage = start(exchange -> respond(exchange, 200, "REDIRECTED-BYTES"));
        HttpServer dl = start(exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + storage.getAddress().getPort() + "/file");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        try {
            Api2Convert client = new Api2Convert("secret-key", Config.builder().maxRetries(0).build());
            OutputFile output = OutputFile.of("o", "http://127.0.0.1:" + dl.getAddress().getPort() + "/result.bin", null);

            byte[] bytes = client.download(output).contents();

            assertEquals("REDIRECTED-BYTES", new String(bytes, StandardCharsets.UTF_8));
        } finally {
            dl.stop(0);
            storage.stop(0);
        }
    }

    @Test
    void uploadIsAuthenticatedWithJobTokenNotAccountKey() {
        Job job = Job.fromMap(parse("""
                {"id":"job-9","token":"tok-abc","server":"https://www2.api2convert.com/v2",
                 "status":{"code":"incomplete"}}
                """));
        http.addJson(200, "{\"id\":\"in-1\",\"type\":\"upload\"}");

        client().jobs().upload(job, "hello".getBytes());

        assertEquals("tok-abc", requestAt(0).header("X-Oc-Token"));
        assertEquals("", requestAt(0).header("X-Oc-Api-Key"), "the account key must never reach the upload server");
        assertFalse(requestAt(0).followRedirects(), "an authenticated request must not follow redirects");
    }

    @Test
    void downloadPasswordDoesNotLeakAcrossACrossHostRedirect() throws IOException {
        // Regression: a password-protected download carries the secret X-Oc-Download-Password custom
        // header. If the (untrusted) storage URL 302s to another host and we followed it, the JDK
        // client would forward that header to the redirect target. A request carrying the secret must
        // therefore NOT follow redirects.
        AtomicInteger evilHits = new AtomicInteger();
        StringBuilder leaked = new StringBuilder();
        HttpServer evil = start(exchange -> {
            evilHits.incrementAndGet();
            String seen = exchange.getRequestHeaders().getFirst("X-Oc-Download-Password");
            if (seen != null) {
                leaked.append(seen);
            }
            respond(exchange, 200, "grabbed");
        });
        HttpServer storage = start(exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + evil.getAddress().getPort() + "/steal");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        try {
            Api2Convert client = new Api2Convert("secret-key", Config.builder().maxRetries(0).build());
            OutputFile output = OutputFile.of("o", "http://127.0.0.1:" + storage.getAddress().getPort() + "/f.pdf", null);

            // The download itself yields the un-followed 302 (no usable body); what matters is that
            // the password never reached the redirect target.
            try {
                client.download(output, "s3cret").contents();
            } catch (RuntimeException ignored) {
                // a 3xx with no body is fine for this assertion
            }

            assertEquals(0, evilHits.get(), "the download password must never be forwarded to a redirect target");
            assertEquals("", leaked.toString());
        } finally {
            storage.stop(0);
            evil.stop(0);
        }
    }

    @Test
    void downloadWithPasswordUsesTheNoRedirectClient() {
        http.addRaw(200, "BYTES".getBytes(StandardCharsets.UTF_8));
        client().download(OutputFile.of("o", "https://dl.example.com/x", "f.pdf"), "s3cret").contents();

        assertFalse(requestAt(0).followRedirects(), "a download carrying a password must not follow redirects");
        assertEquals("s3cret", requestAt(0).header("X-Oc-Download-Password"));
    }

    @Test
    void passwordlessDownloadStillFollowsRedirects() {
        http.addRaw(200, "BYTES".getBytes(StandardCharsets.UTF_8));
        client().download(OutputFile.of("o", "https://dl.example.com/x", "f.pdf")).contents();

        assertTrue(requestAt(0).followRedirects(), "a no-auth download may follow storage redirects");
        assertEquals("", requestAt(0).header("X-Oc-Download-Password"));
    }

    @Test
    void malformedApiSuppliedUriSurfacesAsNetworkException() {
        // A garbled download URL from the API must stay inside the SDK exception hierarchy, not leak a
        // raw IllegalArgumentException from URI parsing.
        Api2Convert client = new Api2Convert("k", Config.builder().maxRetries(0).build());
        OutputFile output = OutputFile.of("o", "https://example.com/a b c", "f.pdf");

        assertThrows(NetworkException.class, () -> client.download(output).contents());
    }

    @Test
    void aSlowUploadIsNotCappedByThePerRequestTimeout() throws IOException {
        // A streamed upload transmits its whole body before the response is received, so a per-request
        // timeout would abort a large/slow upload. The server delays its response well past the (floored
        // 1s) timeout, yet the upload must still succeed — a streamed transfer is bounded only by connect.
        HttpServer server = start(exchange -> {
            exchange.getRequestBody().readAllBytes();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "{\"id\":\"in-1\",\"type\":\"upload\"}");
        });
        try {
            Api2Convert client = new Api2Convert("k", Config.builder().maxRetries(0).timeout(1).build());
            Job job = Job.fromMap(parse("{\"id\":\"job-9\",\"token\":\"tok-abc\",\"server\":\"http://127.0.0.1:"
                    + server.getAddress().getPort() + "\",\"status\":{\"code\":\"incomplete\"}}"));

            InputFile input = client.jobs().upload(job, "hello world".getBytes(StandardCharsets.UTF_8));
            assertEquals("in-1", input.id());
        } finally {
            server.stop(0);
        }
    }

    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
    }

    private static HttpServer start(Handler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } catch (IOException e) {
                exchange.close();
            }
        });
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
