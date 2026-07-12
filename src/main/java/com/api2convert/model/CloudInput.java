package com.api2convert.model;

import com.api2convert.enums.CloudProvider;
import com.api2convert.support.Redaction;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A cloud-storage input descriptor — imports a file the API fetches from customer-owned storage
 * (S3, Azure Blob, FTP, Google Cloud Storage). Attach it via {@code client.convert(cloudInput, ...)}
 * or {@code client.jobs().addInput(jobId, cloudInput)}; it serializes to
 * {@code {type:"cloud", source:<provider>, parameters:{…}, credentials:{…}}}.
 *
 * <p>The per-provider factories carry each provider's required keys as constructor arguments, using
 * the flat/lowercase names the API expects ({@code accesskeyid}, {@code secretaccesskey}, …). They
 * are a <em>structural</em> convenience, not a runtime gate — the builder never rejects a descriptor
 * the permissive server would accept, and {@link #of} keeps a generic escape hatch for optional or
 * forward-compat keys. Google Drive input uses {@code type:gdrive_picker} and is carried by the
 * generic {@code addInput} raw-map path (no typed builder yet).
 *
 * <p><strong>Credentials never render.</strong> {@link #toString()} masks the whole
 * {@code credentials} object to {@code [REDACTED]} and any sensitive {@code parameters} leaf, so a
 * secret can never leak into a log line.
 */
public record CloudInput(String source, Map<String, Object> parameters, Map<String, Object> credentials) {

    public CloudInput {
        parameters = parameters != null ? parameters : Map.of();
        credentials = credentials != null ? credentials : Map.of();
    }

    /** Amazon S3 — {@code parameters:{bucket,file}}, {@code credentials:{accesskeyid,secretaccesskey}}. */
    public static CloudInput amazonS3(String bucket, String file, String accesskeyid, String secretaccesskey) {
        return new CloudInput(CloudProvider.AMAZON_S3.wire(),
                ordered("bucket", bucket, "file", file),
                ordered("accesskeyid", accesskeyid, "secretaccesskey", secretaccesskey));
    }

    /** Azure Blob Storage — {@code parameters:{container,file}}, {@code credentials:{accountname,accountkey}}. */
    public static CloudInput azure(String container, String file, String accountname, String accountkey) {
        return new CloudInput(CloudProvider.AZURE.wire(),
                ordered("container", container, "file", file),
                ordered("accountname", accountname, "accountkey", accountkey));
    }

    /** FTP — {@code parameters:{host,file}}, {@code credentials:{username,password}}. */
    public static CloudInput ftp(String host, String file, String username, String password) {
        return new CloudInput(CloudProvider.FTP.wire(),
                ordered("host", host, "file", file),
                ordered("username", username, "password", password));
    }

    /** Google Cloud Storage — {@code parameters:{projectid,bucket,file}}, {@code credentials:{keyfile}}. */
    public static CloudInput googleCloud(String projectid, String bucket, String file, String keyfile) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("projectid", projectid);
        params.put("bucket", bucket);
        params.put("file", file);
        return new CloudInput(CloudProvider.GOOGLE_CLOUD.wire(), params, ordered("keyfile", keyfile, null, null));
    }

    /** Generic escape hatch — any provider plus free-form {@code parameters} / {@code credentials}. */
    public static CloudInput of(CloudProvider source, Map<String, Object> parameters, Map<String, Object> credentials) {
        return new CloudInput(source.wire(), parameters, credentials);
    }

    /** Generic escape hatch keyed by a raw provider string (e.g. a provider the enum doesn't know yet). */
    public static CloudInput of(String source, Map<String, Object> parameters, Map<String, Object> credentials) {
        return new CloudInput(source, parameters, credentials);
    }

    /** The wire descriptor: {@code {type:"cloud", source, parameters, credentials}}. */
    public Map<String, Object> toDescriptor() {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("type", "cloud");
        descriptor.put("source", source);
        descriptor.put("parameters", parameters);
        descriptor.put("credentials", credentials);
        return descriptor;
    }

    private static Map<String, Object> ordered(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k1, v1);
        if (k2 != null) {
            map.put(k2, v2);
        }
        return map;
    }

    @Override
    public String toString() {
        return "CloudInput{source=" + source
                + ", parameters=" + Redaction.maskSensitive(parameters)
                + ", credentials=" + Redaction.REDACTED + "}";
    }
}
