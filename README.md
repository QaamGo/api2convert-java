# API2Convert Java SDK

[![CI](https://github.com/QaamGo/api2convert-java/actions/workflows/ci.yml/badge.svg)](https://github.com/QaamGo/api2convert-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.api2convert/api2convert-java)](https://central.sonatype.com/artifact/com.api2convert/api2convert-java)
![Java](https://img.shields.io/badge/java-%E2%89%A5%2017-orange)
![License](https://img.shields.io/badge/license-MIT-green)

The official Java client for the [API2Convert](https://www.api2convert.com) file-conversion API.
Convert, compress and transform **images, documents, audio, video, ebooks, archives and CAD** — and
run operations like OCR, merge, thumbnail and website capture — in one line of code.

```java
Api2Convert client = new Api2Convert("YOUR_API_KEY");

client.convert("invoice.docx", "pdf").save("invoice.pdf");
```

That single call creates a job, uploads your file, starts it, waits for it to finish and gives you
back a result you can save. No polling loops, no manual upload handling.

## Requirements

- Java 17+
- One runtime dependency: [Jackson](https://github.com/FasterXML/jackson) (`jackson-databind`).
  The HTTP layer uses the JDK's built-in `java.net.http.HttpClient` — no extra HTTP dependency.

## Install

Maven:

```xml
<dependency>
  <groupId>com.api2convert</groupId>
  <artifactId>api2convert-java</artifactId>
  <version>10.2.1</version>
</dependency>
```

Gradle:

```kotlin
implementation("com.api2convert:api2convert-java:10.2.1")
```

Get an API key from the [API2Convert dashboard / documentation](https://www.api2convert.com/documentation).

## Quick start

```java
import com.api2convert.Api2Convert;
import java.util.Map;

// Reads the API2CONVERT_API_KEY environment variable when no key is passed.
Api2Convert client = new Api2Convert("YOUR_API_KEY");

// 1) From a local file
client.convert("photo.png", "jpg").save("photo.jpg");

// 2) From a URL
client.convert("https://example.com/photo.png", "jpg").save("photo.jpg");

// 3) With conversion options (discover them via client.options("jpg"))
client.convert("photo.png", "jpg", Map.of("quality", 85, "width", 1280, "height", 720))
      .save("out/");   // the processed-file directory
```

`convert(input, to, options)` — `input` is a **local path `String`, a public URL, a `Path`, a
`byte[]` or an `InputStream`**; `to` is the **target format**; `options` are the **conversion
options** for that target. Less-common controls live on `ConvertOptions` (so they can never collide
with an open-ended API option): `category`, `timeout`, `outputIndex`, `filename`, `downloadPassword`.
The returned `ConversionResult` lets you:

```java
ConversionResult result = client.convert("report.docx", "pdf");

result.save("report.pdf");       // stream to a file
result.save("downloads/");       // ...or a directory (keeps the server filename)
byte[] content = result.contents();  // ...or get the raw bytes
String url = result.url();       // ...or just the download URL
```

## Password-protect the result

Pass a `downloadPassword` and the output is locked behind it. The SDK remembers the password and
sends it automatically when you download — you don't pass it again:

```java
import com.api2convert.ConvertOptions;

ConversionResult result = client.convert("statement.docx", "pdf", null,
        new ConvertOptions().downloadPassword("hunter2"));

result.save("statement.pdf");    // the password is applied for you
```

The download URL still needs the password from anywhere else (a browser, cURL, another process),
via the `X-Api2convert-Download-Password` header. When you already hold an `OutputFile` — e.g. from the Jobs
API — hand the password to `download()`:

```java
client.download(output, "hunter2").save("out/");
```

## Asynchronous conversions & webhooks

For long-running jobs, start the conversion and get notified via a webhook instead of waiting:

```java
import com.api2convert.AsyncOptions;

Job job = client.convertAsync("movie.mov", "mp4", null,
        new AsyncOptions().callback("https://your-app.example.com/webhooks/api2convert"));
```

In your webhook handler, verify and parse the callback:

```java
import com.api2convert.Api2Convert;
import com.api2convert.exception.SignatureVerificationException;
import com.api2convert.webhook.WebhookEvent;

byte[] payload = request.rawBody();                 // the RAW body
String signature = request.header("X-Oc-Signature");

try {
    WebhookEvent event = Api2Convert.webhooks().constructEvent(payload, signature, "YOUR_WEBHOOK_SECRET");
    Job job = event.job();
    // ... react to job.status().code() ...
} catch (SignatureVerificationException e) {
    // respond 400
}
```

> Signed webhooks are being rolled out. Until they are enabled for your account no signature is
> sent — call `Api2Convert.webhooks().parse(payload)` (or pass an empty secret) to deserialize the
> callback without verifying.

## Error handling

Every failure is an unchecked exception extending `com.api2convert.exception.Api2ConvertException`:

```java
import com.api2convert.exception.*;

try {
    client.convert("photo.png", "jpg").save("photo.jpg");
} catch (ValidationException e) {
    // bad target / option — e.getMessage() explains
} catch (AuthenticationException e) {
    // bad or missing API key
} catch (RateLimitException e) {
    // too many requests — retry after e.getRetryAfter() seconds
} catch (ConversionFailedException e) {
    // the job failed — inspect e.errors()
}
```

| Exception | When |
|---|---|
| `AuthenticationException` | 401 / 403 — bad or missing key |
| `PaymentRequiredException` | 402 — no remaining quota |
| `ValidationException` | 400 / 422 — invalid request (e.g. unknown target) |
| `NotFoundException` | 404 — resource doesn't exist |
| `RateLimitException` | 429 — exposes `getRetryAfter()` |
| `ServerException` | 5xx |
| `ConversionFailedException` | the job reached `failed`; exposes `getJob()` and `errors()` |
| `TimeoutException` | the job didn't finish within the poll timeout |
| `SignatureVerificationException` | a webhook payload failed verification |

Transient failures (429, 5xx, network errors) are **retried automatically** with jittered
exponential backoff. A non-idempotent `POST` (e.g. creating a job) is never blindly retried, so a
transient error can't create a duplicate job — pass an idempotency key to make it retry-safe:
`client.jobs().create(payload, "my-idempotency-key")`.

## Power user: the full job API

`convert()` is sugar over the Jobs API. Drop down to it for compound jobs, merges, presets, custom
polling or job chaining:

```java
Job job = client.jobs().create(Map.of(
        "process", false,
        "conversion", List.of(Map.of("target", "pdf", "options", Map.of("pdf_a", true)))));

client.jobs().upload(job, "contract.docx");                 // local file
client.jobs().addInput(job.id(), Map.of(                    // ...or a URL
        "type", "remote", "source", "https://example.com/appendix.docx"));

client.jobs().start(job.id());
Job done = client.jobs().await(job.id(), 120);              // poll to completion (120s timeout)

for (OutputFile output : done.output()) {
    client.download(output).save("out/");
}
```

Available resources: `jobs()`, `conversions()` (the catalog + option discovery), `presets()`,
`stats()`, `contracts()`.

Discover the valid options for any target:

```java
Map<String, Object> options = client.options("jpg");   // -> { quality: {...}, width: {...}, ... }
```

> Note: the poll-to-completion method is `jobs().await(...)` (not `wait`, which is reserved by
> `java.lang.Object`). This is the only public name adapted to Java idiom.

## Configuration

```java
import com.api2convert.http.Config;

Api2Convert client = new Api2Convert("YOUR_API_KEY", Config.builder()
        .timeout(30)          // per-request network timeout (seconds)
        .maxRetries(2)        // automatic retries for transient failures
        .pollInterval(1.0)    // first poll interval when waiting (seconds)
        .pollMaxInterval(5.0) // backoff cap (seconds)
        .pollTimeout(300)     // give up waiting after this many seconds
        .build());
```

Bring your own HTTP transport by implementing `com.api2convert.http.HttpSender` and passing it as
the third constructor argument.

## Security — never publish your API key

- **Never hard-code or commit your API key.** Load it from the environment (`API2CONVERT_API_KEY`)
  or a secrets manager.
- In CI, store it as a **masked & protected** variable and never print it to logs.
- Treat the per-job upload **token** and your **webhook signing secret** with the same care.
- The SDK never logs your key/token and never puts them in exception messages. Authenticated
  requests never follow a redirect (a redirect could otherwise forward the key to another host);
  only the self-contained, no-auth download path follows redirects.
- If a key is ever exposed, **revoke and rotate it** in the API2Convert dashboard immediately.

See [`SECURITY.md`](SECURITY.md).

## Development

```bash
mvn verify        # compile + run the offline unit tests
```

Live conformance tests run against the real API when `API2CONVERT_API_KEY` is set (they auto-skip
otherwise):

```bash
API2CONVERT_API_KEY=... mvn verify -DexcludedGroups=
```

The [live conformance suite](src/test/java/com/api2convert/ConversionConformanceTest.java) doubles
as an executable, end-to-end tour of the SDK: it runs the same 20 documented examples end to end
(plus two negative scenarios — an invalid target is a typed validation error, and a bad key is a
typed auth error that never leaks the credential). Each test mirrors one runnable file in
[`examples/`](examples/) and one guide on api2convert.com.

It runs automatically against the real API on every release tag (see
[`.github/workflows/live-conformance.yml`](.github/workflows/live-conformance.yml)), so a published
version is always verified end to end.

### Runnable examples

Each file in [`examples/`](examples/) is a self-contained program that reads the key from
`API2CONVERT_API_KEY` (and honors `API2CONVERT_BASE_URL`). Build the jar (`mvn -B package`), then run
one with the SDK + Jackson on the classpath, e.g.:

```bash
API2CONVERT_API_KEY=your-key java -cp "target/classes:libs/*" Quickstart
```

| Guide | Example |
|---|---|
| Quick start | [`Quickstart.java`](examples/Quickstart.java) |
| Convert files | [`ConvertFiles.java`](examples/ConvertFiles.java) |
| Uploading files | [`UploadingFiles.java`](examples/UploadingFiles.java) |
| The job lifecycle | [`JobLifecycle.java`](examples/JobLifecycle.java) |
| Add a watermark | [`AddWatermark.java`](examples/AddWatermark.java) |
| Create thumbnails | [`CreateThumbnails.java`](examples/CreateThumbnails.java) |
| Compress files | [`CompressFiles.java`](examples/CompressFiles.java) |
| Create archives | [`CreateArchives.java`](examples/CreateArchives.java) |
| Create hashes | [`CreateHashes.java`](examples/CreateHashes.java) |
| Extract assets | [`ExtractAssets.java`](examples/ExtractAssets.java) |
| File analysis | [`FileAnalysis.java`](examples/FileAnalysis.java) |
| Compare files | [`CompareFiles.java`](examples/CompareFiles.java) |
| Capture a website | [`CaptureWebsite.java`](examples/CaptureWebsite.java) |
| Audio operations | [`AudioOperations.java`](examples/AudioOperations.java) |
| Image operations | [`ImageOperations.java`](examples/ImageOperations.java) |
| Webhooks | [`Webhooks.java`](examples/Webhooks.java) |
| Presets | [`Presets.java`](examples/Presets.java) |
| Statistics | [`Statistics.java`](examples/Statistics.java) |
| Rate limits & contracts | [`RateLimits.java`](examples/RateLimits.java) |
| Authentication | [`Authentication.java`](examples/Authentication.java) |

This SDK is hand-written and kept in sync with the API by an AI agent — see [`AGENTS.md`](AGENTS.md)
and [`docs/SDK_CONTRACT.md`](docs/SDK_CONTRACT.md). Notable changes are recorded in
[`docs/CHANGELOG.md`](docs/CHANGELOG.md).

## License

MIT — see [`LICENSE`](LICENSE).
