package com.api2convert.exception;

import com.api2convert.model.Job;

/**
 * A job did not reach a terminal status within the configured poll timeout.
 *
 * <p>The job is still running server-side — re-fetch it later with
 * {@code client.jobs().get(job.id())} or raise the timeout.
 */
public class TimeoutException extends Api2ConvertException {

    private final transient Job job;

    public TimeoutException(Job job, int timeoutSeconds) {
        super("Timed out after " + timeoutSeconds + "s waiting for job " + job.id()
                + " to finish (last status: " + job.status().code() + ").");
        this.job = job;
    }

    /** The job as last observed before the timeout elapsed. */
    public Job getJob() {
        return job;
    }
}
