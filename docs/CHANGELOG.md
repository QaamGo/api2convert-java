# Changelog

All notable changes to this package are documented here. This project adheres to
[Semantic Versioning](https://semver.org/).

## [10.3.2] - 2026-08-07

Dependency security update. No API changes.

- Bumped Jackson 2.18.2 → 2.22.1, clearing nine advisories across `jackson-databind` and
  `jackson-core` (three HIGH: `GHSA-j3rv-43j4-c7qm`, `GHSA-rmj7-2vxq-3g9f`, `GHSA-r7wm-3cxj-wff9`).
  None were reachable through this SDK — it uses a plain `ObjectMapper` with no default typing,
  no `@JsonView`/`@JsonIgnore` and no async parser — but the exact pin could drag a consumer's
  Jackson down under Maven's nearest-wins resolution.
- Test-scope only: JUnit 5.11.4 → 6.1.2, plus current Maven compiler/surefire/javadoc/source/gpg
  plugins. No effect on the published artifact.
- Fixed `Api2Convert.VERSION`, which still read `10.2.1` and so reported a stale `User-Agent`
  for the 10.3.0 and 10.3.1 releases. The release tag guard only compared the tag to `pom.xml`,
  which is why the drift went unnoticed.
- CI now runs an OSV dependency audit that fails the build on any advisory in the resolved Maven
  tree, including transitives, and Dependabot version updates are enabled.

## [10.2.1] - 2026-07-08

- Lock-step version bump to keep all API2Convert SDKs on 10.2.1. No library/runtime changes since
  10.2.0 (the redirect / download-password hardening already shipped in 10.2.0).
- Expanded the live-conformance suite to seven canonical scenarios and added a runnable example per
  documented guide; upgraded the Central publishing plugin (0.7.0 → 0.11.0).

## [10.2.0] - 2026-07-06

- Initial public release of the API2Convert Java SDK.
