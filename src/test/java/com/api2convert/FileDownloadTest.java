package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.model.OutputFile;
import java.io.IOException;
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
}
