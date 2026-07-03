# Changelog

All notable changes to this package are documented here. This project adheres to
[Semantic Versioning](https://semver.org/).

## [10.2.0] - 2026-07-03

First public release of the official, hand-written Java SDK
(`com.api2convert:api2convert-java`), targeting Java 17+. Behaviour parity with the PHP and Python
SDKs at the same version, per [`docs/SDK_CONTRACT.md`](SDK_CONTRACT.md).

### Core
- One-call `convert(input, to, options)` happy path that hides the create → upload → poll → download
  lifecycle for local files, URLs and streams; returns a `ConversionResult` with `save()` /
  `contents()` / `url()`.
- `convertAsync()` for webhook-driven workflows (sets `notify_status` when a `callback` is given).
- `options(target)` to discover the valid conversion options for a target format.
- Full Jobs API (`jobs()`) plus `conversions()`, `presets()`, `stats()` and `contracts()` resources.
- Webhook verification (`Api2Convert.webhooks()`) with constant-time HMAC-SHA256 over the raw body.

### Reliability & security
- Jittered, capped exponential backoff with `Retry-After` support (clamped); a non-idempotent `POST`
  is retried only with an idempotency key, and only replayable bodies are retried.
- Poll interval floored and total wait capped (monotonic deadline) so no configuration can busy-loop
  or poll unbounded.
- Authenticated requests never follow redirects (no key/token leak); only the no-auth download path
  does. Directory downloads sanitize the API-supplied filename against path traversal.

### Implementation
- Immutable `record` DTOs with defensive hydration; one runtime dependency (Jackson) used purely as
  a JSON codec; HTTP via the JDK's built-in `java.net.http.HttpClient` (pluggable `HttpSender`).
- Java-idiom note: the poll-to-completion method is `jobs().await(...)` (the contract's `wait` is
  reserved by `java.lang.Object`).
