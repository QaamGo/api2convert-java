# Releasing

Publishing a new version to **Maven Central** is fully automated by GitHub Actions
([`.github/workflows/release.yml`](../.github/workflows/release.yml)). A human never touches the
Sonatype Portal for a normal release — pushing a version tag does everything.

> **Maven Central releases are permanent.** A published version can never be overwritten or
> deleted. The tag is the deliberate gate; the version can only go up.

## Cut a release

1. Bump the version in [`pom.xml`](../pom.xml) (`<version>`) and `Api2Convert.VERSION`, per SemVer
   (additive spec change → minor; breaking public-surface change → major). Add a `docs/CHANGELOG.md`
   entry. Commit and push to `main`.
2. Tag the release commit and push the tag:
   ```bash
   git tag -a v10.2.1 -m "Release 10.2.1"
   git push origin v10.2.1
   ```

That's it. The `Release` workflow then, on the tagged commit:

1. Verifies the tag (`vX.Y.Z`) matches the `pom.xml` version and is not a `SNAPSHOT` — a mismatch
   fails the build before anything is published.
2. Builds the jar, the sources jar and the javadoc jar (the `release` profile in `pom.xml`).
3. GPG-signs every artifact.
4. Uploads to the Central Portal, waits for validation, and **auto-publishes** (`autoPublish=true`,
   `waitUntil=published`). The job succeeds only once the version is actually live.

Watch it under the repo's **Actions → Release** tab. The
[Maven Central badge](https://central.sonatype.com/artifact/com.api2convert/api2convert-java) in the
README flips to the new version once Central's index syncs (usually a few minutes to an hour after
the job goes green).

## One-time setup (already wired; credentials are the maintainer's)

The pipeline reads four **masked repository secrets** — never committed:

| Secret | What it is |
|--------|------------|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user-token username |
| `MAVEN_CENTRAL_TOKEN` | Central Portal user-token password |
| `MAVEN_GPG_PRIVATE_KEY` | ASCII-armored GPG private key (public half published to a keyserver) |
| `MAVEN_GPG_PASSPHRASE` | passphrase for that GPG key |

Prerequisites, done once by a maintainer:

- A [central.sonatype.com](https://central.sonatype.com) account.
- The `com.api2convert` namespace verified there (DNS `TXT` record on `api2convert.com`).
- The four secrets set on the GitHub repo (`gh secret set …` or the repo Settings UI).

The secret discipline (keys/tokens only from masked secrets, never committed) is spelled out in
[`AGENTS.md`](../AGENTS.md).
