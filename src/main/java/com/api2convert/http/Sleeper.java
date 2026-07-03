package com.api2convert.http;

/**
 * Pluggable sleep used between retries and job polls. The default sleeps for real; tests inject a
 * no-op (or recording) implementation so waits are instant and assertable.
 */
@FunctionalInterface
public interface Sleeper {

    void sleep(double seconds);

    /** The production sleeper. Restores the interrupt flag rather than swallowing interruption. */
    static Sleeper real() {
        return seconds -> {
            long millis = (long) Math.round(seconds * 1000);
            if (millis <= 0) {
                return;
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
