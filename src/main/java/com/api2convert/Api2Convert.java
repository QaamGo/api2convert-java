package com.api2convert;

import com.api2convert.http.Config;
import com.api2convert.http.HttpSender;
import com.api2convert.http.JdkHttpSender;
import com.api2convert.http.Sleeper;
import com.api2convert.http.Transport;
import com.api2convert.model.Job;
import com.api2convert.model.OutputFile;
import com.api2convert.resource.ContractsResource;
import com.api2convert.resource.ConversionsResource;
import com.api2convert.resource.JobsResource;
import com.api2convert.resource.PresetsResource;
import com.api2convert.resource.StatsResource;
import com.api2convert.upload.FileUploader;
import com.api2convert.webhook.WebhookVerifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API2Convert client — convert, compress and transform files with one call.
 *
 * <p>Quick start:
 * <pre>{@code
 * Api2Convert client = new Api2Convert("YOUR_API_KEY");
 * client.convert("invoice.docx", "pdf").save("invoice.pdf");
 * }</pre>
 *
 * <p>{@code convert()} hides the multi-step job lifecycle (create -&gt; upload -&gt; start -&gt; poll
 * -&gt; download). For full control, use {@link #jobs()} and the other resources.
 */
public final class Api2Convert implements AutoCloseable {

    public static final String VERSION = "10.2.0";

    private static final Pattern HTTP_URL = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);

    private final Transport transport;
    private final JobsResource jobs;
    private final ConversionsResource conversions;
    private final PresetsResource presets;
    private final StatsResource stats;
    private final ContractsResource contracts;
    private final HttpSender httpSender;

    /** Construct with an API key (falls back to the {@code API2CONVERT_API_KEY} env var). */
    public Api2Convert(String apiKey) {
        this(apiKey, null, null, null);
    }

    /** Construct with an API key and tuning {@link Config}. */
    public Api2Convert(String apiKey, Config config) {
        this(apiKey, config, null, null);
    }

    /** Construct with your own {@link HttpSender} (e.g. to plug in a different HTTP client). */
    public Api2Convert(String apiKey, Config config, HttpSender httpSender) {
        this(apiKey, config, httpSender, null);
    }

    /** Full constructor. The {@code sleeper} seam is used by tests to make retry/poll waits instant. */
    Api2Convert(String apiKey, Config config, HttpSender httpSender, Sleeper sleeper) {
        String key = resolveKey(apiKey);
        Config cfg = config != null ? config : Config.defaults();
        this.httpSender = httpSender != null ? httpSender : new JdkHttpSender(cfg.timeout());
        Sleeper slp = sleeper != null ? sleeper : Sleeper.real();

        this.transport = new Transport(this.httpSender, cfg, key, slp);
        FileUploader uploader = new FileUploader(transport);
        this.jobs = new JobsResource(transport, uploader);
        this.conversions = new ConversionsResource(transport);
        this.presets = new PresetsResource(transport);
        this.stats = new StatsResource(transport);
        this.contracts = new ContractsResource(transport);
    }

    private static String resolveKey(String apiKey) {
        String key = apiKey != null && !apiKey.isEmpty() ? apiKey : System.getenv("API2CONVERT_API_KEY");
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(
                    "No API key provided. Pass it to the constructor or set the "
                            + "API2CONVERT_API_KEY environment variable.");
        }
        return key;
    }

    // --- one-call convenience -------------------------------------------------------------------

    public ConversionResult convert(Object input, String to) {
        return convert(input, to, null, null);
    }

    public ConversionResult convert(Object input, String to, Map<String, Object> options) {
        return convert(input, to, options, null);
    }

    /**
     * Convert a file and wait for the result.
     *
     * <p>Hand it a local path, a public URL, or an open stream, name the target format, and get back
     * a result you can {@code save()}.
     *
     * @param input   a local path {@code String}, a URL ({@code ^https?://}), a {@code Path}, a
     *                {@code byte[]} or an {@code InputStream}
     * @param to      target format, e.g. {@code pdf}, {@code jpg}, {@code mp4}
     * @param options target-specific conversion options (discover via {@link #options}); may be null
     * @param opts    optional less-common controls (category, timeout, output index, ...); may be null
     */
    public ConversionResult convert(Object input, String to, Map<String, Object> options, ConvertOptions opts) {
        ConvertOptions o = opts != null ? opts : new ConvertOptions();
        Job job = startConversion(input, to, options, o.category, null, o.filename, o.downloadPassword);
        Job done = jobs.await(job.id(), o.timeout);
        return new ConversionResult(done, transport, o.outputIndex != null ? o.outputIndex : 0, o.downloadPassword);
    }

    public Job convertAsync(Object input, String to) {
        return convertAsync(input, to, null, null);
    }

    public Job convertAsync(Object input, String to, Map<String, Object> options) {
        return convertAsync(input, to, options, null);
    }

    /**
     * Start a conversion without waiting. Pass a {@code callback} URL (via {@link AsyncOptions}) to
     * be notified, or poll later with {@code jobs().get(job.id())} / {@code jobs().await(job.id())}.
     */
    public Job convertAsync(Object input, String to, Map<String, Object> options, AsyncOptions opts) {
        AsyncOptions o = opts != null ? opts : new AsyncOptions();
        return startConversion(input, to, options, o.category, o.callback, o.filename, o.downloadPassword);
    }

    /** A {@link FileDownload} for an output file: {@code client.download(out).save("./out/")}. */
    public FileDownload download(OutputFile output) {
        return download(output, null);
    }

    public FileDownload download(OutputFile output, String downloadPassword) {
        return new FileDownload(transport, output, downloadPassword);
    }

    /**
     * Discover the valid options (type / enum / default / range) for a target format:
     * {@code client.options("jpg")}. Pass {@code category} to disambiguate if needed.
     */
    public Map<String, Object> options(String target) {
        return conversions.options(target);
    }

    public Map<String, Object> options(String target, String category) {
        return conversions.options(target, category);
    }

    // --- resources ------------------------------------------------------------------------------

    public JobsResource jobs() {
        return jobs;
    }

    public ConversionsResource conversions() {
        return conversions;
    }

    public PresetsResource presets() {
        return presets;
    }

    public StatsResource stats() {
        return stats;
    }

    public ContractsResource contracts() {
        return contracts;
    }

    /**
     * Webhook verifier — usable without a configured client, e.g. in a controller:
     * {@code Api2Convert.webhooks().constructEvent(rawBody, signatureHeader, secret)}.
     */
    public static WebhookVerifier webhooks() {
        return new WebhookVerifier();
    }

    @Override
    public void close() {
        if (httpSender instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    /**
     * Build + start a job from a file/URL/stream input. Shared by {@code convert()} and
     * {@code convertAsync()}: a URL becomes a single remote-input job started immediately; a local
     * file/stream is staged, uploaded, then started.
     */
    private Job startConversion(Object input, String to, Map<String, Object> options,
                                String category, String callback, String filename, String downloadPassword) {
        Map<String, Object> conversion = new LinkedHashMap<>();
        conversion.put("target", to);
        if (category != null) {
            conversion.put("category", category);
        }
        if (options != null && !options.isEmpty()) {
            conversion.put("options", options);
        }

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("conversion", List.of(conversion));
        if (callback != null) {
            job.put("callback", callback);
            job.put("notify_status", true);
        }
        if (downloadPassword != null) {
            job.put("download_passwords", List.of(downloadPassword));
        }

        if (input instanceof String source && HTTP_URL.matcher(source).find()) {
            job.put("process", true);
            Map<String, Object> remote = new LinkedHashMap<>();
            remote.put("type", "remote");
            remote.put("source", source);
            job.put("input", List.of(remote));
            return jobs.create(job);
        }

        job.put("process", false);
        Job created = jobs.create(job);
        jobs.upload(created, input, filename);
        return jobs.start(created.id());
    }
}
