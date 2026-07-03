package com.api2convert;

import com.api2convert.http.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test case: builds an {@link Api2Convert} client backed by an in-memory {@link FakeHttpSender},
 * so tests never touch the network, and a recording sleeper so retry/poll waits are instant and
 * assertable.
 */
abstract class A2CTestCase {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected FakeHttpSender http;
    protected List<Double> slept;

    @BeforeEach
    void baseSetUp() {
        http = new FakeHttpSender();
        slept = new ArrayList<>();
    }

    protected Api2Convert client() {
        return client(Config.defaults());
    }

    protected Api2Convert client(Config config) {
        return new Api2Convert("test-key", config, http, seconds -> slept.add(seconds));
    }

    protected Recorded requestAt(int index) {
        return new Recorded(http.requests.get(index));
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> parse(String json) {
        try {
            return (Map<String, Object>) MAPPER.readValue(json, Object.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Thin read-only view over a recorded request with convenient accessors. */
    protected static final class Recorded {
        private final FakeHttpSender.Recorded delegate;

        Recorded(FakeHttpSender.Recorded delegate) {
            this.delegate = delegate;
        }

        String method() {
            return delegate.method();
        }

        String uri() {
            return delegate.uri();
        }

        boolean followRedirects() {
            return delegate.followRedirects();
        }

        String header(String name) {
            return delegate.header(name);
        }

        String bodyString() {
            return delegate.bodyString();
        }

        Map<String, Object> bodyJson() {
            return delegate.bodyJson();
        }
    }
}
