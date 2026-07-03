package com.api2convert.exception;

import com.api2convert.model.Job;
import com.api2convert.model.JobMessage;
import java.util.List;

/**
 * The job reached the {@code failed} (or {@code canceled}) status. The originating {@link Job} is
 * attached so you can inspect its errors and warnings.
 */
public class ConversionFailedException extends Api2ConvertException {

    private final transient Job job;

    public ConversionFailedException(Job job) {
        super(buildMessage(job));
        this.job = job;
    }

    /** The failed job, including its {@code errors} and {@code warnings}. */
    public Job getJob() {
        return job;
    }

    /** The job's errors (may be empty if the API gave no detail). */
    public List<JobMessage> errors() {
        return job.errors();
    }

    private static String buildMessage(Job job) {
        List<JobMessage> errors = job.errors();
        if (!errors.isEmpty()) {
            JobMessage first = errors.get(0);
            String code = first.code() != null ? " (code " + first.code() + ")" : "";
            return "Conversion failed: " + first.message() + code;
        }
        String info = job.status().info();
        return "Conversion failed" + (info != null ? ": " + info : ".");
    }
}
