package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.exception.NetworkException;
import com.api2convert.http.Config;
import com.api2convert.http.HttpSender;
import com.api2convert.http.Response;
import com.api2convert.model.OutputFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDownloadTest extends A2CTestCase {

    @TempDir
    Path dir;

    @Test
    void savingToDirectoryUsesTheApiFilename() throws IOException {
        http.addRaw(200, "PDF-BYTES".getBytes(StandardCharsets.UTF_8));
        OutputFile output = OutputFile.of("o", "https://dl.example.com/x", "result.pdf");

        String path = client().download(output).save(dir.toString() + "/");

        assertEquals(dir.resolve("result.pdf").toString(), path);
        assertArrayEquals("PDF-BYTES".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(Path.of(path)));
    }

    @Test
    void traversalFilenameCannotEscapeTheTargetDirectory() {
        http.addRaw(200, "DATA".getBytes(StandardCharsets.UTF_8));
        OutputFile output = OutputFile.of("o", "https://dl.example.com/x", "../../evil.txt");

        String path = client().download(output).save(dir.toString() + "/");

        // The API-supplied name is reduced to a bare basename and stays inside the dir.
        assertEquals(dir.resolve("evil.txt").toString(), path);
        assertTrue(Files.exists(dir.resolve("evil.txt")));
        assertFalse(Files.exists(dir.getParent().getParent().resolve("evil.txt")));
    }

    @Test
    void fallsBackToOutputWhenFilenameIsDotOnly() {
        http.addRaw(200, "DATA".getBytes(StandardCharsets.UTF_8));
        OutputFile output = new OutputFile(null, "https://dl.example.com/x", "..", null, null, null, null, Map.of());

        String path = client().download(output).save(dir.toString() + "/");

        assertEquals(dir.resolve("output").toString(), path);
    }

    @Test
    void noFollowDownloadReceiving302RaisesAndWritesNoFile() {
        // A password-protected download is pinned no-follow so the secret header can't be forwarded.
        // The 3xx that comes back is the storage redirect page, not the file: it must raise a
        // NetworkException, never be silently streamed to disk.
        http.addRaw(302, new byte[0], Map.of("Location", "https://storage.example.com/real"));
        OutputFile output = OutputFile.of("o", "https://dl.example.com/x", "result.pdf");
        Path target = dir.resolve("result.pdf");

        assertThrows(NetworkException.class, () -> client().download(output, "s3cret").save(target.toString()));

        assertFalse(requestAt(0).followRedirects(), "a download carrying a password must not follow redirects");
        assertFalse(Files.exists(target), "an unexpected redirect must not leave a file on disk");
    }

    @Test
    void midWriteFailureLeavesNoFileAtTarget() {
        // A read failure part-way through streaming must not leave a truncated file masquerading as a
        // complete download; the partial target is removed before the error propagates.
        HttpSender failing = request -> new Response() {
            private int served;

            @Override
            public int status() {
                return 200;
            }

            @Override
            public String header(String name) {
                return "";
            }

            @Override
            public InputStream body() {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("connection reset mid-stream");
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        if (served == 0) {
                            served = len;
                            b[off] = 'A';
                            return 1;
                        }
                        throw new IOException("connection reset mid-stream");
                    }
                };
            }
        };
        Api2Convert client = new Api2Convert("k", Config.defaults(), failing, seconds -> slept.add(seconds));
        OutputFile output = OutputFile.of("o", "https://dl.example.com/x", "result.pdf");
        Path target = dir.resolve("result.pdf");

        assertThrows(Api2ConvertException.class, () -> client.download(output).save(target.toString()));

        assertFalse(Files.exists(target), "a mid-write failure must leave no partial file at the target");
    }
}
