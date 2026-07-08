# Voice grading Cloud Function proxy: four single-purpose HTTP functions, Gemini via Vertex AI, Firestore-doc entitlement stub

## Decision

The premium voice-grading backend (design doc: `docs/design/premium-voice-grading-pipeline.md`) is implemented as four separate Firebase Cloud Functions (2nd gen, Node.js 20, TypeScript), deployed from `functions/` at the repo root:

- `entitlement` (`GET`) — verifies the caller's Firebase ID token, reads `users/{uid}/entitlement/premium`, returns `{is_premium}`.
- `transcribe` (`POST`, multipart) — audio in, ElevenLabs Scribe transcript out. No grading.
- `sanitizeAndGrade` (`POST`, JSON) — typed transcript in, single Gemini call out: `{sanitized_transcript, grade, feedback}`.
- `gradeVoiceAnswer` (`POST`, multipart) — the real production path: audio in, calls Scribe then feeds the transcript into the same Gemini call `sanitizeAndGrade` uses, same JSON shape out.

Each function is its own `onRequest` export rather than one function with internal routing, because Cloud Functions' default URL scheme (`https://<region>-<project>.cloudfunctions.net/<function-name>`) already matches the relative paths `VoiceGradingRetrofitService` calls (`gradeVoiceAnswer`, `transcribe`, `sanitizeAndGrade`, `entitlement`) — no Express router or Hosting rewrites needed to get that path shape.

Grading LLM is Gemini via Vertex AI, not Claude/OpenAI/OpenRouter (all three were live options per the design doc's "Open decisions"). Vertex AI runs in the same GCP project as Firestore/Cloud Functions already — no separate API key or vendor account, auth is the function's own runtime service account plus `roles/aiplatform.user`.

Premium entitlement is a real per-request Firestore check (`users/{uid}/entitlement/premium`, `isPremium: boolean`), not a stub that always passes — but the document itself is populated by hand (console/Admin SDK) rather than by a Play Billing sync, since that sync (RTDN → Firestore) is explicitly deferred to a separate design pass. The enforcement mechanism is real; the population mechanism is manual until that follow-up lands.

## Context

Calling ElevenLabs Scribe and a grading LLM directly from the Android app was already rejected in the design doc — any key shipped in the APK is extractable by decompiling it. A backend is mandatory purely to hold those two secrets server-side. Given the app already runs on Firebase/Firestore, a Cloud Function was chosen over standing up a separate server (Cloud Run, a VM) to avoid new infrastructure to operate.

The four-endpoint split mirrors the debug Voice screen's five testable blocks (VAD, capture, obfuscation are on-device only; transcription, sanitize+grade, and entitlement each need a server round-trip) — each server-side block gets its own endpoint so it can be exercised and inspected in isolation before the full pipeline is wired together, per the design doc's "Implementation strategy for blocked/inaccessible dependencies" rule (fake or build the real thing, never leave a stage half-stubbed).

## Alternatives considered

**Single Cloud Function with Express-style internal routing** (`/gradeVoiceAnswer`, `/transcribe`, etc. as sub-routes of one function) — rejected. Would need either Firebase Hosting rewrites or an Express app mounted inside one function to get clean sub-paths; four independent functions get the same URL shape for free from the platform's own naming convention, at the cost of four small deploy units instead of one.

**Cloud Run instead of Cloud Functions** — rejected in the design doc already (stateless request/response fits Cloud Functions; no need for the long-lived-connection story Cloud Run would justify).

**Claude or OpenAI for grading** — rejected for this iteration. Neither has any infrastructure synergy with the existing Firebase/GCP project; both would need an independent API key/account for no offsetting benefit over Gemini. Left open as a later swap — `lib/grading.ts` is the only file that would change.

**OpenRouter for multi-model grading A/B testing** — rejected for now, not because it's a bad idea (design doc flags it as a live option) but because standing up a comparative-grading harness is a separate scope from getting one real vendor working end-to-end first.

**Always-true entitlement stub** (skip the Firestore check entirely until Play Billing sync exists) — rejected. Would violate the design doc's core invariant ("Entitlement enforcement": server-side check before any paid API call, never trust the client). A manually-populated Firestore doc keeps the real check in place; only the population mechanism is a placeholder.

## Consequences

- Full setup/deploy walkthrough, secret management, and the entitlement doc's interim manual-population process are documented in `functions/README.md` — this ADR is the "why", that file is the "how."
- Swapping the grading vendor later (Claude/OpenAI/OpenRouter) touches only `functions/src/lib/grading.ts`; the JSON contract `{sanitized_transcript, grade, feedback}` and every caller of it stay unchanged.
- Swapping entitlement from manual-doc to real Play Billing/RTDN sync later touches only what *writes* `users/{uid}/entitlement/premium` (a new Cloud Function trigger) — `functions/src/lib/entitlement.ts` (what *reads* it) and the Android app do not change.
- Deploying `firestore.rules` alongside this feature closed a pre-existing gap: the ruleset previously in production was a single blanket `match /{document=**} { allow read, write: if request.auth != null }`, which let any signed-in user read/write any other user's documents — including `entitlement` itself. The rules now deployed from this repo (`firestore.rules`, tracked going forward instead of console-only) scope every `users/{uid}/**` path to its own owner and grant the `entitlement` subcollection no client rule at all (default-deny; only the Cloud Function's Admin SDK, which bypasses rules, may touch it).
- Four independent functions means four independent cold-start/deploy units — acceptable at this scale (low request volume, no shared mutable state between them), revisit only if deploy/ops overhead becomes a real cost.
