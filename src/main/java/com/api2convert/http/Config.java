package com.api2convert.http;

/**
 * Immutable client configuration (the tuning knobs). Build via {@link #builder()} so you only set
 * what you need; everything has a sensible default, and {@link Builder#build()} clamps the poll
 * knobs so no configuration can busy-loop or poll unbounded.
 */
public final class Config {

    public static final String DEFAULT_BASE_URL = "https://api.api2convert.com/v2";

    /**
     * Hard floor for the job-poll interval. A caller-supplied 0 or negative value is raised to this
     * so the poll loop can never busy-spin the API (see the 2017 {@code sleep(0)} self-DDOS fix in
     * the legacy SDK).
     */
    public static final double MIN_POLL_INTERVAL = 0.5;

    /**
     * Hard ceiling for the total job-poll timeout (4 hours), mirroring the legacy SDK's
     * MAX_WAITING_TIME. A misconfigured or hostile-large timeout degrades to a bounded wait instead
     * of an unbounded poll.
     */
    public static final int MAX_POLL_TIMEOUT = 14400;

    private final String baseUrl;
    private final int timeout;
    private final int maxRetries;
    private final double pollInterval;
    private final double pollMaxInterval;
    private final int pollTimeout;

    private Config(String baseUrl, int timeout, int maxRetries,
                   double pollInterval, double pollMaxInterval, int pollTimeout) {
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.pollInterval = pollInterval;
        this.pollMaxInterval = pollMaxInterval;
        this.pollTimeout = pollTimeout;
    }

    public static Config defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Satcore/API base URL, e.g. {@code https://api.api2convert.com/v2} (no trailing slash). */
    public String baseUrl() {
        return baseUrl;
    }

    /** Per-request network timeout, in seconds (never below 1). */
    public int timeout() {
        return timeout;
    }

    /** Automatic retries for transient failures (429 / 5xx / network). */
    public int maxRetries() {
        return maxRetries;
    }

    /** First poll interval when waiting for a job, in seconds (never below {@link #MIN_POLL_INTERVAL}). */
    public double pollInterval() {
        return pollInterval;
    }

    /** Upper bound the poll interval backs off to, in seconds (never below {@link #pollInterval()}). */
    public double pollMaxInterval() {
        return pollMaxInterval;
    }

    /** How long to wait for a job to finish before giving up, in seconds (capped at {@link #MAX_POLL_TIMEOUT}). */
    public int pollTimeout() {
        return pollTimeout;
    }

    /** Mutable builder; {@link #build()} is the single clamping entry point. */
    public static final class Builder {
        private String baseUrl = DEFAULT_BASE_URL;
        private int timeout = 30;
        private int maxRetries = 2;
        private double pollInterval = 1.0;
        private double pollMaxInterval = 5.0;
        private int pollTimeout = 300;

        public Builder baseUrl(String baseUrl) {
            if (baseUrl != null && !baseUrl.isEmpty()) {
                this.baseUrl = baseUrl;
            }
            return this;
        }

        public Builder timeout(int seconds) {
            this.timeout = seconds;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }

        public Builder pollInterval(double seconds) {
            this.pollInterval = seconds;
            return this;
        }

        public Builder pollMaxInterval(double seconds) {
            this.pollMaxInterval = seconds;
            return this;
        }

        public Builder pollTimeout(int seconds) {
            this.pollTimeout = seconds;
            return this;
        }

        public Config build() {
            // Clamp so a caller value can neither busy-loop (interval floor) nor poll unbounded
            // (timeout ceiling), the max interval is never below the starting interval, and the
            // per-request timeout is never disabled (0 = "no timeout" is an unbounded-hang landmine).
            double interval = Math.max(MIN_POLL_INTERVAL, pollInterval);
            double maxInterval = Math.max(interval, pollMaxInterval);
            int totalTimeout = Math.min(MAX_POLL_TIMEOUT, Math.max(0, pollTimeout));
            String trimmedBaseUrl = baseUrl.endsWith("/")
                    ? baseUrl.substring(0, baseUrl.length() - 1)
                    : baseUrl;

            return new Config(
                    trimmedBaseUrl,
                    Math.max(1, timeout),
                    Math.max(0, maxRetries),
                    interval,
                    maxInterval,
                    totalTimeout);
        }
    }
}
