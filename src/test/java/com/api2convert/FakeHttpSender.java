package com.api2convert;

import com.api2convert.http.HttpSender;
import com.api2convert.http.Request;
import com.api2convert.http.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link HttpSender} for offline tests — the Java analog of php-http/mock-client and
 * httpx.MockTransport. Queue canned responses (or a transport exception) in order, then inspect the
 * requests the SDK actually sent. Streaming bodies are materialized so tests can assert on them.
 */
final class FakeHttpSender implements HttpSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    final List<Recorded> requests = new ArrayList<>();
    private final Deque<Object> queue = new ArrayDeque<>();

    void addJson(int status, String json) {
        addJson(status, json, Map.of());
    }

    void addJson(int status, String json, Map<String, String> headers) {
        Map<String, String> merged = new LinkedHashMap<>();
        merged.put("Content-Type", "application/json");
        merged.putAll(headers);
        queue.add(new Canned(status, merged, json.getBytes(StandardCharsets.UTF_8)));
    }

    void addRaw(int status, byte[] body) {
        addRaw(status, body, Map.of());
    }

    void addRaw(int status, byte[] body, Map<String, String> headers) {
        queue.add(new Canned(status, new LinkedHashMap<>(headers), body));
    }

    void addException(IOException exception) {
        queue.add(exception);
    }

    @Override
    public Response send(Request request) throws IOException {
        byte[] body;
        if (request.body() != null) {
            body = request.body();
        } else if (request.streamBody() != null) {
            try (InputStream in = request.streamBody().get()) {
                body = in.readAllBytes();
            }
        } else {
            body = new byte[0];
        }
        requests.add(new Recorded(request.method(), request.uri(),
                new LinkedHashMap<>(request.headers()), body, request.followRedirects()));

        Object next = queue.poll();
        if (next == null) {
            throw new IllegalStateException("FakeHttpSender: no canned response queued for "
                    + request.method() + " " + request.uri());
        }
        if (next instanceof IOException io) {
            throw io;
        }
        return (Canned) next;
    }

    /** A queued response. */
    private record Canned(int statusCode, Map<String, String> headers, byte[] bytes) implements Response {
        @Override
        public int status() {
            return statusCode;
        }

        @Override
        public String header(String name) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return "";
        }

        @Override
        public InputStream body() {
            return new ByteArrayInputStream(bytes);
        }
    }

    /** A request the SDK sent. */
    record Recorded(String method, String uri, Map<String, String> headers, byte[] body, boolean followRedirects) {

        String header(String name) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return "";
        }

        String bodyString() {
            return new String(body, StandardCharsets.UTF_8);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> bodyJson() {
            try {
                Object decoded = MAPPER.readValue(body, Object.class);
                return decoded instanceof Map ? (Map<String, Object>) decoded : Map.of();
            } catch (IOException e) {
                return Map.of();
            }
        }
    }
}
