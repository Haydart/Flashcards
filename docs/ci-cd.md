# CI/CD (Bitrise)

Operational reference for the Bitrise pipeline. Rationale and alternatives considered: [ADR-0032](adr/0032-bitrise-cicd-gitflow.md). This doc changes as the pipeline evolves (e.g. the Play Store step in Phase 6 below) — the ADR does not.

## Branch flow

```
feature/*  --PR-->  develop  --PR-->  main  --(future PR)-->  release/*
              [gate]           [gate]
```

| Event | Workflow | What runs |
|---|---|---|
| PR into `develop` or `main` | `pr-check` | `./gradlew staticAnalysis` + `./gradlew test`. Required GitHub status check — merge is blocked on failure. Branch protection requires the PR to be up to date with base and disallows admin bypass, so this is the *only* Bitrise build per PR — no separate post-merge check. |
| Push to `main` | `deploy-internal` | Builds debug + release APKs, signs release, pushes debug to Firebase App Distribution's `internal-debug` group and release to `internal-release`. |
| Push to `release/*` | *(not yet implemented — see Phase 6)* | Will build an AAB and upload to Play Store. |

No instrumented (`androidTest`) tests exist yet, so no emulator step is configured. Add one to `pr-check` if/when instrumented tests land.

## Secrets / Generic File Storage (Bitrise)

| Name | Type | Purpose |
|---|---|---|
| `google-services.json` | Generic File Storage | Firebase config for the Android app. Contains both the release app (`com.rossomak.flashcards`) and debug app (`com.rossomak.flashcards.debug`) entries. Gitignored, per-dev/CI. |
| `release.keystore` | Generic File Storage | Release signing key. **Back up outside Bitrise too** (password manager) — unrecoverable if lost, no Play App Signing fallback configured. |
| `RELEASE_STORE_FILE` | Secret env var / Gradle `-P` | Path to `release.keystore` once placed on the CI runner. |
| `RELEASE_STORE_PASSWORD` | Secret env var | Release keystore store password. |
| `RELEASE_KEY_ALIAS` | Secret env var | Release key alias (`flashcards-release`). |
| `RELEASE_KEY_PASSWORD` | Secret env var | Release key password. Same value as `RELEASE_STORE_PASSWORD` — keystore is PKCS12, which requires store and key password to match. |
| `GOOGLE_WEB_CLIENT_ID` | Secret env var | Currently only in local `local.properties`; needed in CI for the same `buildConfigField`. |
| Firebase App Distribution service account JSON | Generic File Storage | Dedicated service account, scoped to App Distribution admin only — **not** the same credential `functions/` uses for its own deploys. |

## Versioning

- `versionCode` = `git rev-list --count HEAD` — total commit count, monotonic, no tagging required.
- `versionName` = `MAJOR.MINOR.<commit count>` — `MAJOR`/`MINOR` are hand-edited constants in `app/build.gradle.kts`; the patch segment is the same commit count as `versionCode` and climbs across the whole repo history (doesn't reset per release).
- To cut a new `MAJOR`/`MINOR`: edit the constants directly in `app/build.gradle.kts`.

## Signing

- `app/build.gradle.kts` `signingConfigs.release` reads `storeFile`/`storePassword`/`keyAlias`/`keyPassword` via a `releaseSigningProperty()` helper: Gradle `-P` flags first (what Bitrise passes), falling back to `local.properties` (what local dev uses, same pattern as `GOOGLE_WEB_CLIENT_ID`). No repo-committed defaults either way — `assembleRelease` without either source set will fail by design.
- Debug builds use the default debug keystore (unchanged, no setup needed).
- **If the release keystore is ever regenerated**, its new SHA-1 must be re-registered in the Firebase console (Project settings → Your apps), or Google Sign-In (`feature:auth`) breaks on release-signed builds.

## Debug/release package split

- `buildTypes.debug` sets `applicationIdSuffix = ".debug"`, so debug installs as `com.rossomak.flashcards.debug` alongside a release install of `com.rossomak.flashcards` on the same device — same signing-cert mismatch that would otherwise block co-installing them is sidestepped by using distinct package names instead.
- This requires **two Firebase Android apps** under the one Firebase project: `com.rossomak.flashcards` (release, pre-existing) and `com.rossomak.flashcards.debug` (added for this). Each needs its own SHA-1 (and SHA-256 only if Dynamic Links/App Check are ever added) registered against it in the Firebase console.
- Both apps' config lives in the single `google-services.json` (keyed internally by `package_name`) — one file, no per-build-type file swapping needed.
- `app/src/debug/res/values/strings.xml` overrides `app_name` to "Flashcards Debug" so the two are visually distinguishable on-device too.

## GitHub integration

- Bitrise GitHub App installed on `Haydart/Flashcards`.
- Branch protection on `develop` and `main`: `pr-check` status check required before merge.

Implementation progress (per-item, granular) is tracked in [docs/temp/bitrise-cicd-implementation-plan.md](temp/bitrise-cicd-implementation-plan.md), not here.
