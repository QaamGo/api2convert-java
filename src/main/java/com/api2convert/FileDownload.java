package com.api2convert;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.http.Transport;
import com.api2convert.model.OutputFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * A downloadable output file. Returned by {@code client.download(output)} and used internally by
 * {@link ConversionResult}.
 */
public final class FileDownload {

    private final Transport transport;
    private final OutputFile output;
    private final String downloadPassword;

    /**
     * @param downloadPassword password remembered from {@code convert()} / {@code client.download()},
     *                         sent automatically on download; overridable per call
     */
    public FileDownload(Transport transport, OutputFile output, String downloadPassword) {
        this.transport = transport;
        this.output = output;
        this.downloadPassword = downloadPassword;
    }

    /** The self-contained download URL (no auth required). */
    public String url() {
        return output.uri();
    }

    public String save(String pathOrDir) {
        return save(pathOrDir, null);
    }

    /**
     * Stream the file to disk.
     *
     * @param pathOrDir        a file path, or a directory (the API filename is used)
     * @param downloadPassword overrides the password remembered from conversion time
     * @return the path the file was written to
     */
    public String save(String pathOrDir, String downloadPassword) {
        Path target = resolveTarget(pathOrDir);
        Path dir = target.getParent();
        try {
            if (dir != null) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new Api2ConvertException("Could not create directory: " + dir + ": " + e.getMessage(), e);
        }

        try (InputStream source = transport.download(output.uri(), headers(downloadPassword));
             OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[1 << 16];
            int read;
            while ((read = source.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // A read/write failure mid-stream leaves a truncated file at the target. Callers that
            // retry (or hand the path to another process) must never see a partial download that
            // masquerades as complete, so remove it before surfacing the error.
            deleteQuietly(target);
            throw new Api2ConvertException("Could not write file: " + target + ": " + e.getMessage(), e);
        }

        return target.toString();
    }

    public byte[] contents() {
        return contents(null);
    }

    /**
     * Download the file and return its contents (loads into memory).
     *
     * @param downloadPassword overrides the password remembered from conversion time
     */
    public byte[] contents(String downloadPassword) {
        try (InputStream source = transport.download(output.uri(), headers(downloadPassword))) {
            return source.readAllBytes();
        } catch (IOException e) {
            throw new Api2ConvertException("Could not read the download: " + e.getMessage(), e);
        }
    }

    private Path resolveTarget(String pathOrDir) {
        boolean looksLikeDir = Files.isDirectory(Path.of(pathOrDir))
                || pathOrDir.endsWith("/")
                || pathOrDir.endsWith(java.io.File.separator);

        if (looksLikeDir) {
            String name = firstNonNull(safeName(output.filename()), safeName(output.id()), "output");
            return Path.of(pathOrDir).resolve(name);
        }
        return Path.of(pathOrDir);
    }

    /**
     * Reduce an API-supplied name to a bare filename safe to append to a target directory.
     * {@code output.filename} / {@code output.id} come straight from the API JSON, so a value like
     * {@code ../../etc/cron.d/evil} (or one containing separators or a NUL byte) must never escape
     * the directory the caller chose. Returns null when nothing usable remains, so the caller falls
     * back.
     */
    private String safeName(String name) {
        if (name == null) {
            return null;
        }
        // Normalize Windows separators and drop NUL, then take the last path segment so every
        // directory component and any leading "../" is stripped on all platforms; trim surrounding space.
        String normalized = name.replace("\0", "").replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String base = (slash >= 0 ? normalized.substring(slash + 1) : normalized).trim();
        if (base.isEmpty() || base.equals(".") || base.equals("..")) {
            return null;
        }
        return base;
    }

    private Map<String, String> headers(String downloadPassword) {
        String password = downloadPassword != null ? downloadPassword : this.downloadPassword;
        return password != null ? Map.of("X-Oc-Download-Password", password) : Map.of();
    }

    /** Best-effort removal of a partially-written target after a download failure. */
    private static void deleteQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // best effort; the write already failed and we are propagating that error
        }
    }

    private static String firstNonNull(String a, String b, String c) {
        if (a != null) {
            return a;
        }
        if (b != null) {
            return b;
        }
        return c;
    }
}
