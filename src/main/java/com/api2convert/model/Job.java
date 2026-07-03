package com.api2convert.model;

import com.api2convert.enums.JobStatus;
import com.api2convert.support.Data;
import java.util.List;
import java.util.Map;

/**
 * A conversion job — the central API2Convert resource.
 *
 * <p>Returned by every job operation. {@link #server()} and {@link #token()} are needed to upload
 * local files; {@link #output()} holds the produced files once {@link #isCompleted()}. {@link #raw()}
 * keeps the full decoded response for fields not surfaced as typed properties.
 */
public record Job(
        String id,
        Status status,
        String token,
        String server,
        String callback,
        List<Conversion> conversion,
        List<InputFile> input,
        List<OutputFile> output,
        List<JobMessage> errors,
        List<JobMessage> warnings,
        Map<String, Object> raw) {

    public static Job fromMap(Map<String, Object> data) {
        return new Job(
                Data.string(data.get("id")),
                Status.fromMap(Data.object(data.get("status"))),
                Data.nullableString(data.get("token")),
                Data.nullableString(data.get("server")),
                Data.nullableString(data.get("callback")),
                Data.mapObjects(data.get("conversion"), Conversion::fromMap),
                Data.mapObjects(data.get("input"), InputFile::fromMap),
                Data.mapObjects(data.get("output"), OutputFile::fromMap),
                Data.mapObjects(data.get("errors"), JobMessage::fromMap),
                Data.mapObjects(data.get("warnings"), JobMessage::fromMap),
                data);
    }

    public boolean isCompleted() {
        return JobStatus.COMPLETED.wire().equals(status.code());
    }

    public boolean isFailed() {
        return JobStatus.FAILED.wire().equals(status.code());
    }

    /** The job was canceled server-side — terminal, and produced no output. */
    public boolean isCanceled() {
        return JobStatus.CANCELED.wire().equals(status.code());
    }

    /** Finished (completed, failed or canceled) and will not change further. */
    public boolean isTerminal() {
        return JobStatus.isTerminalCode(status.code());
    }
}
