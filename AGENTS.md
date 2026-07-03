# AGENTS — maintaining the API2Convert Java SDK

This SDK is **hand-written** (not generated from OpenAPI) and kept in sync with the API by a human
**or an AI agent**. This file is the playbook. The model: a committed spec snapshot is the diff
baseline, a fixed behavior contract protects the ergonomics, and the JUnit suite is the guardrail.

It is one of three official ports (PHP, Python, Java) that all implement the same language-agnostic
contract in [`docs/SDK_CONTRACT.md`](docs/SDK_CONTRACT.md).

## Why hand-written

The conversion flow is multi-step (create → upload → poll → download) and the **upload step is not
in the OpenAPI spec at all**, so a generator cannot produce a usable client. We optimise for a
junior-friendly surface — one-call `convert()` — and use AI to keep it current.

## Repo layout

| Path | What it is |
|------|------------|
| `src/main/java/com/api2convert/Api2Convert.java` | The client + the `convert()` / `convertAsync()` façade. **Hand-authored.** |
| `src/main/java/com/api2convert/ConversionResult.java`, `FileDownload.java` | Result + download helpers. **Hand-authored.** |
| `src/main/java/com/api2convert/upload/FileUploader.java` | Multipart upload to the per-job server. **Hand-authored** (not in the spec). |
| `src/main/java/com/api2convert/resource/*` | One class per API tag (Jobs, Conversions, Presets, Stats, Contracts). **Derived** from the spec. |
| `src/main/java/com/api2convert/model/*`, `enums/*` | Typed record DTOs / enums. **Derived** from the spec. |
| `src/main/java/com/api2convert/http/*` | Transport: auth, retries/backoff, error mapping, the `HttpSender` seam. Mostly stable infrastructure. |
| `src/main/java/com/api2convert/exception/*` | The typed exception hierarchy. |
| `openapi/api2convert.openapi.json` | **Committed spec snapshot** the SDK targets — the diff baseline. |
| `docs/SDK_CONTRACT.md` | The fixed, language-agnostic public surface + semantics. |
| `src/test/java/com/api2convert/*` | Offline golden tests (`FakeHttpSender`) + live conformance. **The guardrail.** |

## How to update the SDK to a new API version

1. **Refresh the snapshot.** Overwrite `openapi/api2convert.openapi.json` from
   `https://api.api2convert.com/v2/openapi.json` (or `/v2/schema`) and `git diff` it.
2. **Diff it** — new/removed/renamed operations, new fields, new enum values.
3. **Update the DERIVED layer to match the diff, and nothing else:**
   - New/changed fields → update the relevant `model/*` record (`fromMap` + a component).
   - New operation → add a method on the matching `resource/*` class (mirror the existing style).
   - New input/output target types → extend the matching `enums/*`.
4. **Do NOT change the hand-authored public API** (`convert`, `convertAsync`, `download`, upload,
   polling, webhook verification, exception classes) unless `docs/SDK_CONTRACT.md` changes first.
   If a real product change requires it, update the contract in the same change and bump the
   **major** version.
5. **Lint + test (the guardrail):**
   ```bash
   mvn verify        # compiles the whole surface + runs the JUnit suite — all must pass
   ```
   Add or update a golden test for any new behavior. Keep the live conformance test runnable.
6. **Record + version.** Add a `docs/CHANGELOG.md` entry and bump the version in `pom.xml` and
   `Api2Convert.VERSION` per SemVer (additive spec change → minor; breaking public-surface change →
   major).

## Guarantees to uphold (don't break these)

- **Never commit a real API key, token or secret** — not in source, tests, fixtures, examples, CI
  files or commit messages, and never publish one anywhere. Keys come only from environment variables
  (`API2CONVERT_API_KEY`) or masked/protected CI variables; tests use obvious fakes (`test-key`,
  `whsec_test`, ...). The SDK must never log or expose a key/token in errors. Secret-scan before any
  release.
- **The contract is law.** Public method names, signatures and semantics match `docs/SDK_CONTRACT.md`
  across every SDK language. Adapt only to Java idiom (see divergences below).
- **Upload uses the per-job `X-Oc-Token`, never the account key.** There is a test for this.
- **Authenticated requests never follow redirects.** The key/token ride in custom headers that a
  redirect-following client would forward across hosts. Only the no-auth download path follows
  redirects. There is a test for this (`SecurityTest`).
- **`convert()` stays one call** for the common case (path/URL/stream → `to` → `save()`).
- **Transient failures retry; failures surface as typed exceptions.** Never leak a raw HTTP/transport
  error (wrap it in `NetworkException`). A non-idempotent `POST` is never blindly retried.
- **Java 17+, immutable record DTOs, one runtime dependency (Jackson), JDK `HttpClient`.** Don't add
  heavy deps.

## Java-idiom divergences from the contract

The contract fixes names and semantics; these are the *only* places Java deviates, all for idiom:

- **`jobs().await(...)`** is the poll-to-completion method (the contract calls it `wait`). `wait` is
  reserved by `java.lang.Object`, so the Java name is `await` (cf. `CompletableFuture`).
- **Resource accessors are methods** (`jobs()`), like PHP; the "extra" `convert()` controls are a
  fluent `ConvertOptions` / `AsyncOptions` object (Java has no keyword args), kept separate from the
  open-ended options map exactly as the contract requires.
- **Exceptions are unchecked** (extend `RuntimeException`) and named `...Exception` (the contract /
  Python name some `...Error`). Method names are camelCase.
- **Models are `record`s** with a static `fromMap` factory; a `byte[]`/`InputStream`/`Path`/`String`
  is accepted for `input`/upload. `size` fields are `Long` (files can exceed a 32-bit int).

## Conventions

- Models parse defensively via `support/Data` (tolerate missing/extra fields — never throw on a
  surprising payload during hydration). `Job.raw()` keeps the full decoded response.
- Resource methods are thin: build the request, call `Transport`, hydrate a model.
- Keep the README quickstart copy-pasteable; if you change the happy path, update the README example.
