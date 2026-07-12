package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.api2convert.model.CloudInput;
import com.api2convert.model.Conversion;
import com.api2convert.model.InputFile;
import com.api2convert.model.Job;
import com.api2convert.model.OutputTarget;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Canonical cloud-connector parity fixtures (D-5). Reproduces the three scenarios from
 * {@code api2convert-cloud-connector-parity-fixtures.md} identically to every other SDK: fixture 1
 * asserts what the SDK serializes on create, fixture 2 asserts read hydration. Redaction (fixture 3)
 * lives in {@link CloudRedactionTest}.
 */
class CloudConnectorTest extends A2CTestCase {

    // --- Fixture 1 — create payload -------------------------------------------------------------

    @Test
    void fixture1_convertSerializesCloudInputAndOutputTarget() {
        http.addJson(201, "{\"id\":\"job-1\",\"status\":{\"code\":\"incomplete\"}}");
        http.addJson(200, "{\"id\":\"job-1\",\"status\":{\"code\":\"completed\"}}");

        CloudInput input = CloudInput.amazonS3("my-bucket", "in/photo.png", "AKIA_TEST", "SECRET_TEST");
        OutputTarget target = OutputTarget.of("ftp",
                orderedParams(), orderedFtpCreds());

        // Output target attached via the convert() outputTargets control (never the options map).
        client().convert(input, "jpg", null, new ConvertOptions().outputTargets(target));

        assertCreateBody(requestAt(0).bodyJson());
    }

    @Test
    void fixture1_rawCreateProducesByteIdenticalOutputTarget() {
        http.addJson(201, "{\"id\":\"job-1\",\"status\":{\"code\":\"incomplete\"}}");

        CloudInput input = CloudInput.amazonS3("my-bucket", "in/photo.png", "AKIA_TEST", "SECRET_TEST");
        OutputTarget target = OutputTarget.of("ftp", orderedParams(), orderedFtpCreds());

        Map<String, Object> conversion = new LinkedHashMap<>();
        conversion.put("target", "jpg");
        conversion.put("output_target", List.of(target.toDescriptor()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("process", true);
        payload.put("input", List.of(input.toDescriptor()));
        payload.put("conversion", List.of(conversion));

        client().jobs().create(payload);

        // Assertion 4: the raw jobs().create path produces the same input/output_target as convert().
        assertCreateBody(requestAt(0).bodyJson());
    }

    @SuppressWarnings("unchecked")
    private static void assertCreateBody(Map<String, Object> body) {
        // 1) a cloud input is a started job (like a remote URL), not staged/uploaded.
        assertEquals(Boolean.TRUE, body.get("process"));

        // 2) input[0] = flat/lowercase cloud descriptor, keys exactly as emitted by the factory.
        Map<String, Object> in0 = (Map<String, Object>) ((List<?>) body.get("input")).get(0);
        assertEquals("cloud", in0.get("type"));
        assertEquals("amazons3", in0.get("source"));
        assertEquals(Map.of("bucket", "my-bucket", "file", "in/photo.png"), in0.get("parameters"));
        assertEquals(Map.of("accesskeyid", "AKIA_TEST", "secretaccesskey", "SECRET_TEST"), in0.get("credentials"));

        // 3) conversion[0].output_target[0] = {type, parameters, credentials} and NO status key.
        Map<String, Object> conv0 = (Map<String, Object>) ((List<?>) body.get("conversion")).get(0);
        Map<String, Object> ot0 = (Map<String, Object>) ((List<?>) conv0.get("output_target")).get(0);
        assertEquals("ftp", ot0.get("type"));
        assertEquals(Map.of("host", "ftp.example.com", "file", "/out/photo.jpg"), ot0.get("parameters"));
        assertEquals(Map.of("username", "u", "password", "p"), ot0.get("credentials"));
        assertFalse(ot0.containsKey("status"), "status is read-only and must never be serialized on create");
    }

    private static Map<String, Object> orderedParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("host", "ftp.example.com");
        params.put("file", "/out/photo.jpg");
        return params;
    }

    private static Map<String, Object> orderedFtpCreds() {
        Map<String, Object> creds = new LinkedHashMap<>();
        creds.put("username", "u");
        creds.put("password", "p");
        return creds;
    }

    // --- Fixture 2 — read hydration -------------------------------------------------------------

    @Test
    void fixture2_hydratesCloudInputAndOutputTargetAsRawValues() {
        Job job = Job.fromMap(parse("""
                {
                  "id": "job-1",
                  "status": { "code": "completed" },
                  "input": [
                    { "id": "in-1", "type": "cloud", "source": "amazons3", "status": "ready",
                      "parameters": { "bucket": "my-bucket", "file": "in/photo.png" },
                      "credentials": {} }
                  ],
                  "conversion": [
                    { "id": "c-1", "target": "jpg",
                      "output_target": [
                        { "type": "ftp",
                          "parameters": { "host": "ftp.example.com", "file": "/out/photo.jpg" },
                          "credentials": {}, "status": "uploading" }
                      ] }
                  ]
                }
                """));

        // 1) input source is a raw string; parameters surface.
        InputFile in0 = job.input().get(0);
        assertEquals("amazons3", in0.source());
        assertEquals(Map.of("bucket", "my-bucket", "file", "in/photo.png"), in0.parameters());

        // 2) output target type/status/parameters surface.
        OutputTarget ot0 = job.conversion().get(0).outputTargets().get(0);
        assertEquals("ftp", ot0.type());
        assertEquals("uploading", ot0.status());
        assertEquals(Map.of("host", "ftp.example.com", "file", "/out/photo.jpg"), ot0.parameters());

        // 3) credentials are NOT surfaced (empty), matching the server strip.
        assertTrue(ot0.credentials().isEmpty(), "output-target credentials must not be hydrated");
    }

    @Test
    void fixture2_unknownProviderRoundTripsUntyped() {
        Job job = Job.fromMap(parse("""
                {
                  "id": "job-2",
                  "status": { "code": "completed" },
                  "input": [ { "id": "in-1", "type": "cloud", "source": "r2", "status": "ready",
                               "parameters": { "bucket": "b" } } ],
                  "conversion": [
                    { "id": "c-1", "target": "jpg",
                      "output_target": [ { "type": "r2", "parameters": {}, "status": "waiting" } ] }
                  ]
                }
                """));

        // Forward-compat: an unknown provider string hydrates without any enum parse throwing.
        assertEquals("r2", job.input().get(0).source());
        Conversion conversion = job.conversion().get(0);
        assertEquals("r2", conversion.outputTargets().get(0).type());
        assertEquals("waiting", conversion.outputTargets().get(0).status());
    }
}
