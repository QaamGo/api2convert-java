package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.api2convert.enums.CloudProvider;
import com.api2convert.model.CloudInput;
import com.api2convert.model.OutputTarget;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit coverage for the cloud vocabulary and the per-provider input builders. */
class CloudBuilderTest {

    @Test
    void providerWireValues() {
        assertEquals("amazons3", CloudProvider.AMAZON_S3.wire());
        assertEquals("azure", CloudProvider.AZURE.wire());
        assertEquals("ftp", CloudProvider.FTP.wire());
        assertEquals("gdrive", CloudProvider.GDRIVE.wire());
        assertEquals("googlecloud", CloudProvider.GOOGLE_CLOUD.wire());
        assertEquals("youtube", CloudProvider.YOUTUBE.wire());
        assertEquals(CloudProvider.FTP, CloudProvider.fromWire("ftp"));
        assertNull(CloudProvider.fromWire("r2"), "an unknown provider resolves to null, never throws");
    }

    @Test
    void amazonS3FactoryEmitsFlatLowercaseKeys() {
        Map<String, Object> d = CloudInput.amazonS3("bkt", "in.png", "AKIA", "SEK").toDescriptor();
        assertEquals("cloud", d.get("type"));
        assertEquals("amazons3", d.get("source"));
        assertEquals(Map.of("bucket", "bkt", "file", "in.png"), d.get("parameters"));
        assertEquals(Map.of("accesskeyid", "AKIA", "secretaccesskey", "SEK"), d.get("credentials"));
    }

    @Test
    void azureFactoryEmitsFlatLowercaseKeys() {
        Map<String, Object> d = CloudInput.azure("cont", "in.png", "acc", "k").toDescriptor();
        assertEquals("azure", d.get("source"));
        assertEquals(Map.of("container", "cont", "file", "in.png"), d.get("parameters"));
        assertEquals(Map.of("accountname", "acc", "accountkey", "k"), d.get("credentials"));
    }

    @Test
    void ftpFactoryEmitsFlatLowercaseKeys() {
        Map<String, Object> d = CloudInput.ftp("h", "/f", "u", "p").toDescriptor();
        assertEquals("ftp", d.get("source"));
        assertEquals(Map.of("host", "h", "file", "/f"), d.get("parameters"));
        assertEquals(Map.of("username", "u", "password", "p"), d.get("credentials"));
    }

    @Test
    void googleCloudFactoryEmitsFlatLowercaseKeys() {
        Map<String, Object> d = CloudInput.googleCloud("proj", "bkt", "in.png", "KEYJSON").toDescriptor();
        assertEquals("googlecloud", d.get("source"));
        assertEquals(Map.of("projectid", "proj", "bucket", "bkt", "file", "in.png"), d.get("parameters"));
        assertEquals(Map.of("keyfile", "KEYJSON"), d.get("credentials"));
    }

    @Test
    void genericEscapeHatchAllowsForwardCompatKeys() {
        CloudInput input = CloudInput.of(CloudProvider.AMAZON_S3,
                Map.of("bucket", "b", "region", "eu-central-1"), Map.of("accesskeyid", "AKIA"));
        Map<String, Object> d = input.toDescriptor();
        assertEquals("amazons3", d.get("source"));
        assertEquals(Map.of("bucket", "b", "region", "eu-central-1"), d.get("parameters"));
    }

    @Test
    void outputTargetDescriptorOmitsStatus() {
        OutputTarget target = new OutputTarget("ftp", Map.of("host", "h"), Map.of("password", "p"), "uploading");
        Map<String, Object> d = target.toDescriptor();
        assertEquals("ftp", d.get("type"));
        assertEquals(Map.of("host", "h"), d.get("parameters"));
        assertEquals(Map.of("password", "p"), d.get("credentials"));
        assertFalse(d.containsKey("status"), "status is read-only and never serialized");
    }
}
