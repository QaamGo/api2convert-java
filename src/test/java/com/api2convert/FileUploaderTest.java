package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.exception.Api2ConvertException;
import com.api2convert.model.Job;
import org.junit.jupiter.api.Test;

class FileUploaderTest extends A2CTestCase {

    @Test
    void uploadFailsWhenJobHasNoUploadServerOrToken() {
        // A job created with process=true (or already started) exposes no upload server/token;
        // uploading to it must fail fast, before touching the network.
        Job job = Job.fromMap(parse("{\"id\":\"j\",\"status\":{\"code\":\"incomplete\"}}"));

        Api2ConvertException e = assertThrows(Api2ConvertException.class,
                () -> client().jobs().upload(job, "/tmp/whatever"));
        assertTrue(e.getMessage().contains("no upload server/token"));
    }

    @Test
    void uploadFailsWhenLocalFileDoesNotExist() {
        Job job = Job.fromMap(parse("""
                {"id":"j","server":"https://upload.example.com","token":"tok",
                 "status":{"code":"incomplete"}}
                """));

        Api2ConvertException e = assertThrows(Api2ConvertException.class,
                () -> client().jobs().upload(job, "/no/such/file-6f1c2b.bin"));
        assertTrue(e.getMessage().contains("Input file not found"));
    }
}
