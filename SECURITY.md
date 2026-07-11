# Security Policy

## Reporting a vulnerability

Please **do not** open a public GitHub issue for a security problem in this SDK.

Report it privately through GitHub's **"Report a vulnerability"** button under the repository's
*Security* tab (private vulnerability reporting). If that is unavailable, use the support channels at
<https://www.api2convert.com>. Please avoid disclosing details publicly until a fix has been released.

## Secrets this SDK handles

The library handles three secrets on the caller's behalf — keep all of them out of source control
and configure them via environment variables or a secret manager:

- the **account API key** (`X-Api2convert-Api-Key`) — read from configuration/environment
  (`API2CONVERT_API_KEY`) and sent only over TLS to the API host, never in a URL query string;
- the **per-job upload token** (`X-Api2convert-Token`) — used to authenticate uploads to the per-job upload
  server; the account key is **never** sent there;
- the **webhook signing secret** — used locally to verify callback signatures (HMAC-SHA256 over the
  raw request body, constant-time comparison via `MessageDigest.isEqual`). The signature is delivered
  in the `X-Oc-Signature` header.

## Guarantees

- The SDK never logs a key/token and never places one in an exception message.
- A request that carries **any secret in a custom header never follows HTTP redirects** — a redirect
  could otherwise forward the secret to another host (the JDK client only strips `Authorization` /
  `Cookie` on a cross-host redirect, not arbitrary `X-Api2convert-*` headers). This covers the account key
  (`X-Api2convert-Api-Key`), the per-job upload token (`X-Api2convert-Token`) **and** a download password
  (`X-Api2convert-Download-Password`). Only a plain, passwordless download (`GET output.uri`, which carries no
  secret) follows redirects, so storage/CDN URLs still resolve.
- A directory download uses a sanitized basename derived from the API-supplied filename, so a
  malicious name (e.g. `../../evil`) cannot escape the target directory.
- Transient failures are retried with capped, jittered backoff; a non-idempotent `POST` is never
  blindly retried, so a transient error cannot create a duplicate job.

If a key is ever exposed, revoke and rotate it in the API2Convert dashboard immediately.
