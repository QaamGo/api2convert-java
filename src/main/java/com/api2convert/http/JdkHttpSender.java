package com.api2convert.http;

import com.api2convert.exception.NetworkException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

/**
 * Default {@link HttpSender}, backed by the JDK's {@code java.net.http.HttpClient} (no third-party
 * HTTP dependency).
 *
 * <p>Redirect policy is client-level in the JDK, so this holds two clients: one that never follows
 * redirects (used for every authenticated request — the account key / per-job token ride in custom
 * headers that a redirect-following client would forward across hosts) and one that follows normal
 * redirects (used only for the self-contained, no-auth download path, where storage URLs legitimately
 * redirect). The choice is made per request from {@link Request#followRedirects()}.
 */
public final class JdkHttpSender implements HttpSender {

    private final HttpClient noRedirect;
    private final HttpClient followRedirects;
    private final Duration requestTimeout;

    public JdkHttpSender(int timeoutSeconds) {
        this.requestTimeout = Duration.ofSeconds(Math.max(1, timeoutSeconds));
        this.noRedirect = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.followRedirects = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Response send(Request request) throws IOException {
        HttpClient client = request.followRedirects() ? followRedirects : noRedirect;

        BodyPublisher publisher;
        if (request.streamBody() != null) {
            publisher = BodyPublishers.ofInputStream(request.streamBody());
        } else if (request.body() != null) {
            publisher = BodyPublishers.ofByteArray(request.body());
        } else {
            publisher = BodyPublishers.noBody();
        }

        URI uri;
        try {
            uri = URI.create(request.uri());
        } catch (IllegalArgumentException e) {
            // A malformed URI (e.g. a garbled API-supplied download URL) must surface inside the SDK
            // exception hierarchy, not as a raw IllegalArgumentException. Thrown before the request is
            // sent, so it is not retried.
            throw new NetworkException("Invalid request URI '" + request.uri() + "': " + e.getMessage(), e);
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .method(request.method(), publisher);
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        try {
            HttpResponse<InputStream> response = client.send(builder.build(), BodyHandlers.ofInputStream());
            return new JdkResponse(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }

    private record JdkResponse(HttpResponse<InputStream> response) implements Response {

        @Override
        public int status() {
            return response.statusCode();
        }

        @Override
        public String header(String name) {
            return response.headers().firstValue(name).orElse("");
        }

        @Override
        public InputStream body() {
            return response.body();
        }
    }
}
