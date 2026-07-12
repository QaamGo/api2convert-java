package com.api2convert.resource;

import com.api2convert.exception.ConversionFailedException;
import com.api2convert.exception.TimeoutException;
import com.api2convert.http.Config;
import com.api2convert.http.Transport;
import com.api2convert.model.InputFile;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import com.api2convert.support.Data;
import com.api2convert.upload.FileUploader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Jobs resource — full control over the job lifecycle.
 *
 * <p>Most users only need {@code client.convert()}, which is built on top of these methods. Reach
 * for this resource for compound jobs, merges, presets, custom polling or job chaining.
 */
public final class JobsResource {

    private final Transport transport;
    private final FileUploader uploader;

    public JobsResource(Transport transport, FileUploader uploader) {
        this.transport = transport;
        this.uploader = uploader;
    }

    /** Create a job. Pass {@code "process" -> false} to stage it for uploads, then {@link #start}. */
    public Job create(Map<String, Object> payload) {
        return create(payload, null);
    }

    /**
     * Create a job, optionally with an {@code Idempotency-Key} that makes the create retry-safe.
     *
     * @param payload        job body: {@code conversion}, optional {@code input}, {@code process}, {@code callback}, ...
     * @param idempotencyKey optional key sent as the {@code Idempotency-Key} header (null to omit)
     */
    public Job create(Map<String, Object> payload, String idempotencyKey) {
        Map<String, String> headers = idempotencyKey != null
                ? Map.of("Idempotency-Key", idempotencyKey)
                : Map.of();
        return Job.fromMap(Data.object(transport.request("POST", "/jobs", payload, Map.of(), headers)));
    }

    public Job get(String jobId) {
        return Job.fromMap(Data.object(transport.request("GET", "/jobs/" + Transport.encodeSegment(jobId))));
    }

    /** List the current key's jobs (paginated, 50 per page). */
    public List<Job> list(String status, int page) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        if (status != null) {
            query.put("status", status);
        }
        return Data.mapObjects(transport.request("GET", "/jobs", null, query), Job::fromMap);
    }

    public List<Job> list() {
        return list(null, 1);
    }

    /** Modify a job. The common case — starting a staged job — has the dedicated {@link #start}. */
    public Job update(String jobId, Map<String, Object> payload) {
        return Job.fromMap(Data.object(transport.request("PATCH", "/jobs/" + Transport.encodeSegment(jobId), payload)));
    }

    /** Start processing a staged job ({@code process -> true}). */
    public Job start(String jobId) {
        return update(jobId, Map.of("process", true));
    }

    /** Cancel a job (whether staged or processing). */
    public void cancel(String jobId) {
        transport.request("DELETE", "/jobs/" + Transport.encodeSegment(jobId));
    }

    /**
     * Attach an input by descriptor — e.g. a remote URL:
     * {@code addInput(id, Map.of("type", "remote", "source", "https://..."))}.
     */
    public InputFile addInput(String jobId, Map<String, Object> input) {
        return InputFile.fromMap(Data.object(
                transport.request("POST", "/jobs/" + Transport.encodeSegment(jobId) + "/input", input)));
    }

    /**
     * Attach a cloud-storage input built with {@link com.api2convert.model.CloudInput} — e.g.
     * {@code addInput(id, CloudInput.amazonS3(bucket, file, accesskeyid, secretaccesskey))}.
     */
    public InputFile addInput(String jobId, com.api2convert.model.CloudInput input) {
        return addInput(jobId, input.toDescriptor());
    }

    /**
     * Upload a local file (path {@code String}, {@code Path}, {@code byte[]} or {@code InputStream})
     * to the job's upload server.
     */
    public InputFile upload(Job job, Object file, String filename) {
        return uploader.upload(job, file, filename);
    }

    public InputFile upload(Job job, Object file) {
        return uploader.upload(job, file, null);
    }

    /** Block until the job reaches a terminal status, polling with backoff. */
    public Job await(String jobId) {
        return await(jobId, null, true);
    }

    public Job await(String jobId, Integer timeoutSeconds) {
        return await(jobId, timeoutSeconds, true);
    }

    /**
     * Block until the job reaches a terminal status, polling with backoff.
     *
     * <p>The interval is floored and the total wait is capped (again — {@link Config} already clamps)
     * so no configuration can busy-loop or poll unbounded, and the deadline is a monotonic wall-clock
     * one (not a sum of sleeps), so slow API responses cannot make the real wait exceed the timeout.
     *
     * @param timeoutSeconds overrides the configured poll timeout (clamped to a sane maximum)
     * @param throwOnFailure when true (default), a failed/canceled job throws {@link ConversionFailedException}
     * @throws ConversionFailedException when the job fails/is canceled and {@code throwOnFailure} is true
     * @throws TimeoutException          when the timeout elapses before completion
     */
    public Job await(String jobId, Integer timeoutSeconds, boolean throwOnFailure) {
        Config config = transport.config();

        int timeout = Math.min(Config.MAX_POLL_TIMEOUT,
                Math.max(0, timeoutSeconds != null ? timeoutSeconds : config.pollTimeout()));
        double maxInterval = Math.max(Config.MIN_POLL_INTERVAL, config.pollMaxInterval());
        double interval = Math.max(Config.MIN_POLL_INTERVAL, config.pollInterval());
        long deadline = System.nanoTime() + (long) (timeout * 1_000_000_000L);

        while (true) {
            Job job = get(jobId);

            if ((job.isFailed() || job.isCanceled()) && throwOnFailure) {
                throw new ConversionFailedException(job);
            }
            if (job.isTerminal()) {
                return job;
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException(job, timeout);
            }

            transport.pause(interval);
            interval = Math.min(maxInterval, interval * 1.5);
        }
    }

    /** Outputs produced by the job (use {@link #get} first, or {@link #await}). */
    public List<OutputFile> outputs(String jobId) {
        return Data.mapObjects(
                transport.request("GET", "/jobs/" + Transport.encodeSegment(jobId) + "/output"), OutputFile::fromMap);
    }
}
