package com.api2convert.enums;

import java.util.Set;

/**
 * Well-known job status codes (the {@code status.code} field).
 *
 * <p>The API may introduce further codes; treat any code not listed here as non-terminal. Use
 * {@link #isTerminal()} on a case, or {@link #isTerminalCode(String)} for a raw status string,
 * rather than comparing strings by hand.
 */
public enum JobStatus {
    CREATED("created"),
    INCOMPLETE("incomplete"),
    DOWNLOADING("downloading"),
    QUEUED("queued"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled");

    private final String wire;

    JobStatus(String wire) {
        this.wire = wire;
    }

    private static final Set<JobStatus> TERMINAL = Set.of(COMPLETED, FAILED, CANCELED);

    /** The API's string value for this status. */
    public String wire() {
        return wire;
    }

    /** Resolve a raw status code to a case, or null if it is not a known value. */
    public static JobStatus fromWire(String code) {
        if (code != null) {
            for (JobStatus s : values()) {
                if (s.wire.equals(code)) {
                    return s;
                }
            }
        }
        return null;
    }

    /** A job in a terminal state is finished and will not change further. */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Is the given raw status code a terminal one? Unknown codes are non-terminal. */
    public static boolean isTerminalCode(String code) {
        JobStatus s = fromWire(code);
        return s != null && s.isTerminal();
    }
}
