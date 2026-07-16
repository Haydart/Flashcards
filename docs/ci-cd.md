# CI/CD (Bitrise)

Operational reference for the Bitrise pipeline. Rationale and alternatives considered: [ADR-0032](adr/0032-bitrise-cicd-gitflow.md). This doc changes as the pipeline evolves (e.g. the Play Store step in Phase 6 below) — the ADR does not.

## Branch flow

```
feature/*  --PR-->  develop  --PR-->  main  --(future PR)-->  release/*
              [gate]           [gate]
```

| Event | Workflow | What runs |
|---|---|---|
| PR into `develop` or `main` | `primary-ci` | `./gradlew staticAnalysis` + `./gradlew test`. Required GitHub status check — merge is blocked on failure. |
| Push to `develop` (post-merge) | `develop-check` | Same checks, no distribution. Safety net only. |
| Push to `main` | `deploy-internal` | Builds debug + release APKs, signs release, pushes both to Firebase App Distribution ("internal" tester group). |
| Push to `release/*` | *(not yet implemented — see Phase 6)* | Will build an AAB and upload to Play Store. |

No instrumented (`androidTest`) tests exist yet, so no emulator step is configured. Add one to `primary-ci` if/when instrumented tests land.

## Secrets / Generic File Storage (Bitrise)

| Name | Type | Purpose |
|---|---|---|
| `google-services.json` | Generic File Storage | Firebase config for the Android app. Gitignored, per-dev/CI. |
| `release.keystore` | Generic File Storage | Release signing key. **Back up outside Bitrise too** (password manager) — unrecoverable if lost, no Play App Signing fallback configured. |
| `KEYSTORE_PASSWORD` | Secret env var | Release keystore store password. |
| `KEY_ALIAS` | Secret env var | Release key alias (`flashcards-release`). |
| `KEY_PASSWORD` | Secret env var | Release key password. |
| `GOOGLE_WEB_CLIENT_ID` | Secret env var | Currently only in local `local.properties`; needed in CI for the same `buildConfigField`. |
| Firebase App Distribution service account JSON | Generic File Storage | Dedicated service account, scoped to App Distribution admin only — **not** the same credential `functions/` uses for its own deploys. |

## Versioning

- `versionCode` = `git rev-list --count HEAD` — total commit count, monotonic, no tagging required.
- `versionName` = `MAJOR.MINOR.<commit count>` — `MAJOR`/`MINOR` are hand-edited constants in `app/build.gradle.kts`; the patch segment is the same commit count as `versionCode` and climbs across the whole repo history (doesn't reset per release).
- To cut a new `MAJOR`/`MINOR`: edit the constants directly in `app/build.gradle.kts`.

## Signing

- `app/build.gradle.kts` `signingConfigs.release` reads `storeFile`/`storePassword`/`keyAlias`/`keyPassword` from Gradle properties (`-P` flags), with no repo-committed defaults — local `assembleRelease` without those flags will fail by design.
- Debug builds use the default debug keystore (unchanged, no setup needed).
- **If the release keystore is ever regenerated**, its new SHA-1 must be re-registered in the Firebase console (Project settings → Your apps), or Google Sign-In (`feature:auth`) breaks on release-signed builds.

## GitHub integration

- Bitrise GitHub App installed on `Haydart/Flashcards`.
- Branch protection on `develop` and `main`: `primary-ci` status check required before merge.

## Implementation status

- [ ] Phase 1 — `signingConfig` + versioning added to `app/build.gradle.kts`
- [ ] Phase 2 — Bitrise account created, GitHub App installed, secrets/files uploaded
- [ ] Phase 3 — `primary-ci` workflow + branch protection
- [ ] Phase 4 — `develop-check` workflow
- [ ] Phase 5 — `deploy-internal` workflow
- [ ] Phase 6 — Play Store: `release/*` trigger, `bundleRelease`, Play Console service account, Play Store deploy step
