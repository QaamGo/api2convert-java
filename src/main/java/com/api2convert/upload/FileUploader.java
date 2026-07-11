package com.api2convert.upload;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.http.Request;
import com.api2convert.http.Transport;
import com.api2convert.model.InputFile;
import com.api2convert.model.Job;
import com.api2convert.support.Data;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Uploads a local file to a job's per-job upload server.
 *
 * <p>This step is intentionally hand-written: it is NOT described by the OpenAPI spec. It posts a
 * {@code multipart/form-data} body (field {@code file}) to {@code {job.server}/upload-file/{job.id}}
 * and authenticates with the per-job {@code X-Api2convert-Token} header — <strong>never the account API
 * key</strong>. The body is streamed, so large files do not have to be read into memory.
 */
public final class FileUploader {

    private final Transport transport;

    public FileUploader(Transport transport) {
        this.transport = transport;
    }

    /**
     * @param file     a path {@code String}, a {@link Path}, a {@code byte[]}, or an {@link InputStream}
     * @param filename name advertised to the API (defaults to the path's basename, else {@code "file"})
     */
    public InputFile upload(Job job, Object file, String filename) {
        if (job.server() == null || job.server().isEmpty() || job.token() == null) {
            throw new Api2ConvertException(
                    "Cannot upload: the job has no upload server/token. "
                            + "Create the job with process=false and upload before starting it.");
        }

        Resolved resolved = resolve(file, filename);

        String boundary = "----api2convertBoundary" + Long.toHexString(System.nanoTime());
        byte[] preamble = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + resolved.name + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] epilogue = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        Supplier<InputStream> streamFactory = () -> new SequenceInputStream(Collections.enumeration(List.of(
                new ByteArrayInputStream(preamble),
                resolved.open.get(),
                new ByteArrayInputStream(epilogue))));

        String server = job.server().endsWith("/")
                ? job.server().substring(0, job.server().length() - 1)
                : job.server();
        String url = server + "/upload-file/" + job.id();

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Api2convert-Token", job.token());
        headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);

        Request request = Request.streaming("POST", url, headers, streamFactory);
        return InputFile.fromMap(Data.object(transport.interpret(transport.send(request))));
    }

    private Resolved resolve(Object file, String filename) {
        if (file instanceof String path) {
            return resolvePath(Path.of(path), path, filename);
        }
        if (file instanceof Path path) {
            return resolvePath(path, path.toString(), filename);
        }
        if (file instanceof File f) {
            return resolvePath(f.toPath(), f.getPath(), filename);
        }
        if (file instanceof byte[] bytes) {
            return new Resolved(filename != null ? filename : "file", () -> new ByteArrayInputStream(bytes));
        }
        if (file instanceof InputStream stream) {
            return new Resolved(filename != null ? filename : "file", () -> stream);
        }
        throw new Api2ConvertException(
                "Unsupported upload input: expected a path String, Path, byte[] or InputStream.");
    }

    private Resolved resolvePath(Path path, String display, String filename) {
        if (!Files.isRegularFile(path)) {
            throw new Api2ConvertException("Input file not found: " + display);
        }
        String name = filename != null ? filename : path.getFileName().toString();
        return new Resolved(name, () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private record Resolved(String name, Supplier<InputStream> open) {
    }
}
