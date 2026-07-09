# Voice grading Cloud Functions

Backend for the premium voice-answering pipeline. See `docs/design/premium-voice-grading-pipeline.md`
for the full pipeline design and `docs/adr/0024-voice-grading-cloud-function-proxy.md` for why this
is shaped as four separate functions. This file is the practical "how to set it up / how it works"
reference; the ADR is the "why", kept short on purpose.

## Project

Firebase project: `flashcards-8ad6d` (see `.firebaserc` at repo root). Region: `us-central1` for
every function (`RUNTIME_OPTIONS.region` in `src/index.ts`).

## Layout

```
functions/
  package.json / tsconfig.json      — Node 20, TypeScript, strict
  src/index.ts                      — the 4 exported onRequest functions
  src/lib/auth.ts                   — Firebase ID token verification (401 on failure)
  src/lib/entitlement.ts            — Firestore entitlement read + gate (403 on failure)
  src/lib/multipart.ts              — Busboy parsing of req.rawBody for the two audio endpoints
  src/lib/elevenlabs.ts             — ElevenLabs Scribe STT call
  src/lib/grading.ts                — Vertex AI Gemini sanitize+grade call
  src/lib/httpError.ts              — HttpError(statusCode, message) thrown by any lib fn
```

`admin.initializeApp()` runs once at module load in `index.ts` (Application Default Credentials —
no service account key needed inside the deployed function itself; that's only used locally for
the Admin SDK scripts under `scripts/seed/` and one-off setup commands below).

## Endpoint contract

Mirrors `core/data/.../network/VoiceGradingApi.kt` / `VoiceGradingRetrofitService.kt` exactly —
if you change one side, change the other and re-check both.

| Function | Method | Body | Response | Auth | Entitlement gate |
|---|---|---|---|---|---|
| `entitlement` | GET | — | `{"is_premium": bool}` | required | n/a (this *is* the check) |
| `transcribe` | POST | multipart: `audio` (`answer.wav`) | `{"transcript": string}` | required | required |
| `sanitizeAndGrade` | POST | JSON: `{"question","expected_answer","transcript"}` | `{"sanitized_transcript","grade","feedback"}` | required | required |
| `gradeVoiceAnswer` | POST | multipart: `audio`, `card_id`, `question`, `expected_answer` | `{"sanitized_transcript","grade","feedback"}` | required | required |

Auth: `Authorization: Bearer <Firebase ID token>` — the Android app's `FirebaseAuthTokenInterceptor`
attaches this automatically once `VOICE_GRADING_BASE_URL` is configured (see repo-root
`local.properties`). Every endpoint calls `requireAuthenticatedUid(req)` first; a missing/invalid
token is a `401` before anything else runs (no ElevenLabs/Vertex call is ever made for an
unauthenticated request — confirmed during setup: `curl` with no token returns `401` immediately).

Entitlement gate: `transcribe`, `sanitizeAndGrade`, and `gradeVoiceAnswer` all call
`requirePremiumEntitlement(uid)` right after auth, which reads `users/{uid}/entitlement/premium`
and throws `403` if `isPremium !== true`. `entitlement` itself just reports the same flag without
gating on it.

`card_id` on `gradeVoiceAnswer` is accepted but not used server-side — it's there because the
client's multipart body includes it (see `RetrofitVoiceGradingApi.toAudioPart()` and friends); the
app is the one that persists `{cardId, sanitizedTranscript, gradePercent, feedback}` to Firestore
after getting the response back, not this function.

## One-time setup (from a clean checkout)

All commands below run from the repo root unless noted. No global `firebase-tools` install is
needed — everything goes through `npx firebase-tools`.

1. **Install deps and typecheck**
   ```
   cd functions && npm install && npx tsc --noEmit
   ```

2. **Log in** (interactive browser OAuth — must be run by a human, not scripted)
   ```
   npx firebase-tools login
   npx firebase-tools projects:list   # confirm flashcards-8ad6d shows up, marked (current)
   ```

3. **Confirm Blaze plan.** Cloud Functions v2 (and any outbound network call — ElevenLabs, Vertex
   AI) require the pay-as-you-go Blaze plan; Spark will not deploy these.
   ```
   open "https://console.firebase.google.com/project/flashcards-8ad6d/usage/details"
   ```

4. **Enable the Vertex AI API** (needed for the Gemini grading call — one-time per project):
   ```
   open "https://console.cloud.google.com/apis/library/aiplatform.googleapis.com?project=flashcards-8ad6d"
   ```
   Click Enable. The function's own runtime service account
   (`<project-number>-compute@developer.gserviceaccount.com`, the GCP default compute SA — visible
   in the deploy output) needs `roles/aiplatform.user` to actually call Gemini; on this project it
   already had sufficient access via its default Editor-equivalent role, so no extra IAM grant was
   needed. If a future deploy gets a Vertex AI `PERMISSION_DENIED`, that role is what to check/add
   first (IAM & Admin → grant `roles/aiplatform.user` to that service account).

5. **Set the ElevenLabs API key as a secret** (never in `local.properties`, never in the app, never
   committed anywhere — Secret Manager only):
   ```
   npx firebase-tools functions:secrets:set ELEVENLABS_API_KEY
   ```
   Prompts for the value with hidden input. `src/index.ts` declares it via
   `defineSecret("ELEVENLABS_API_KEY")` and only binds it to the two functions that actually call
   ElevenLabs (`transcribe`, `gradeVoiceAnswer`) — `entitlement` and `sanitizeAndGrade` don't get it
   injected at all, since they never need it.

6. **Build and deploy**
   ```
   cd functions && npm run build
   cd .. && npx firebase-tools deploy --only functions
   ```
   First deploy will also silently enable several supporting APIs (Cloud Build, Artifact Registry,
   Eventarc, Pub/Sub, Cloud Run, Secret Manager) and grant the runtime service account
   `secretAccessor` on the ElevenLabs secret — all visible in the deploy log, nothing to do
   manually. Deploy prints the four function URLs:
   ```
   https://us-central1-flashcards-8ad6d.cloudfunctions.net/{entitlement,transcribe,sanitizeAndGrade,gradeVoiceAnswer}
   ```

7. **Set the container-image cleanup policy** (one-time; otherwise old Cloud Build container images
   accumulate storage cost forever):
   ```
   npx firebase-tools functions:artifacts:setpolicy
   ```

8. **Firestore rules.** `firestore.rules` at the repo root is deployed via:
   ```
   npx firebase-tools deploy --only firestore:rules
   ```
   **Read `docs/adr/0024-voice-grading-cloud-function-proxy.md`'s Consequences section before
   touching this file** — it replaced a blanket `allow read, write: if request.auth != null` that
   had zero per-user scoping. If you add a new top-level or nested collection anywhere in the app,
   it needs its own explicit `match` block here or it will silently 403/permission-deny with no
   matching rule (default deny) — confirm the real Firestore path structure by grepping
   `core/data/.../source/*.kt` for `.collection(...)` calls before writing the rule; don't guess the
   nesting. (Cost of guessing wrong: an entire collection becomes unreadable app-wide until the next
   rules deploy — this happened once during initial setup, `subcategories` was assumed nested under
   `categories` when it's actually a flat top-level collection with a `categoryId` field.)

9. **Entitlement doc for a test account.** Play Billing → Firestore sync is a separate design pass
   (deferred, see design doc + ADR-0024); until it exists, populate the doc by hand for whichever
   account you test with. Find the UID in
   `https://console.firebase.google.com/project/flashcards-8ad6d/authentication/users`, then either
   use the Firestore console UI to create `users/{uid}/entitlement/premium` with field
   `isPremium: true` (boolean), or run this from the repo root (uses the existing
   `firebase-service-account.json` Admin SDK credential already used by `scripts/seed/`):
   ```js
   node -e '
   const admin = require("./functions/node_modules/firebase-admin");
   admin.initializeApp({ credential: admin.credential.cert(require("./firebase-service-account.json")) });
   const uid = "PASTE_UID_HERE";
   admin.firestore().collection("users").doc(uid).collection("entitlement").doc("premium")
     .set({ isPremium: true })
     .then(() => process.exit(0));
   '
   ```

10. **Point the app at the real backend.** In repo-root `local.properties` (gitignored):
    ```
    VOICE_GRADING_BASE_URL=https://us-central1-flashcards-8ad6d.cloudfunctions.net/
    ```
    Rebuild (`./gradlew assembleDebug`), open the debug-only 🛠️ Voice Debug tab, flip each stage's
    real/fake toggle (disabled until the URL above is non-blank), test in order: entitlement (no
    third-party cost) → transcription → sanitize+grade → a real study-session voice answer for the
    full `gradeVoiceAnswer` path.

## Local iteration

- `npm run build:watch` inside `functions/` for a standing `tsc --watch`.
- No Firebase emulator is wired up yet — every test today goes against the real deployed functions
  via the debug screen or `curl`. Adding `firebase emulators:start` for local-only iteration on
  `functions/src` would be a reasonable follow-up if the deploy round-trip becomes a bottleneck.
- Quick manual auth-gate sanity check against the live deployment (no cost — rejected before any
  paid API call):
  ```
  curl -s -o /dev/null -w "%{http_code}\n" https://us-central1-flashcards-8ad6d.cloudfunctions.net/entitlement
  # -> 401
  ```

## Redeploying after a code change

```
cd functions && npm run build && cd .. && npx firebase-tools deploy --only functions
```
Deploys all four; Firebase only actually updates the ones whose source changed. To redeploy a
single function: `--only functions:transcribe` (etc.).

## Secrets and cost hygiene

- `ELEVENLABS_API_KEY` lives only in Secret Manager, bound only to `transcribe` and
  `gradeVoiceAnswer`. Rotate via `functions:secrets:set` again (creates a new version; old versions
  are not auto-deleted — prune manually in Secret Manager console if that matters).
- Set a per-key credit/usage cap on the ElevenLabs dashboard itself (key settings → usage
  restriction) as a second line of defense independent of this function's own entitlement gate.
- Gemini via Vertex AI bills to the same GCP project's normal billing — no separate cap mechanism
  here beyond the entitlement gate and whatever budget alerts you set on the project.
