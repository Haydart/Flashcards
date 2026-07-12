# Voice answering is Rated-mode only, drives its own automatic grading loop, and shares the Fast-mode TTS engine

## Decision

Voice answering (mic-listen-and-grade) applies **exclusively to Rated Study Sessions**. Fast Study Sessions never get voice answering, under any configuration.

Enabling voice answering in a Rated session auto-enables TTS question-reading, running through the **same** `VoiceGateway`/`TtsPlayer` engine Fast mode already uses (same bottom sheet: speed slider, voice picker, play/pause) — not a separate `TextToSpeech` instance. Unlike Fast mode, this engine must **stop after reading the question** and never auto-progress to reading the answer. Once the question finishes, `VoiceAnswerController` starts listening — never before, and never while any TTS (question or grade-feedback notice) is speaking.

When voice answering is on, the grade **replaces** the manual Failed/Partial/Correct self-rating entirely — no rating buttons are shown. This rework stops short of wiring that grade into mastery/XP/Terminal-state persistence (all of which is unbuilt in this codebase today, see Consequences) — it only takes the pipeline as far as: grade computed, feedback spoken, card advances.

Two new tunable constants:
- **8 seconds** of continuous silence after the question finishes → declare no-answer, speak an audible skip notice, advance without a grade for that card.
- **1000ms** fixed delay after the grade-feedback notice finishes speaking → advance to the next card.

Grade-percent bands, used both for feedback tone and as the future Failed/Partial/Correct mapping: **Failed < 40, Partial 40-79, Correct ≥ 80**. On Failed/Partial, the spoken feedback must state what was missed and include the full acceptable answer (the only place the user hears the real answer in this mode). On Correct, feedback is a short affirmation only.

Voice answering is toggled in-session (mic icon/switch after the session has already started, existing consent + mic-permission flow), not chosen upfront on the Preview Study Session Screen alongside the Rated/Fast mode selector.

> **Amended by [ADR-0030](0030-preview-session-settings-sheet.md):** voice answering is now *also* selectable up front, as a row in the Preview Study Session Screen's settings sheet. The in-session toggle remains; the "never chosen upfront on the Preview screen" restriction in the paragraph above no longer holds. All other decisions in this ADR stand.

## Context

The original implementation gated `VoiceAnswerController`'s activation on `StudySessionViewModel.isVoiceActive`, which only ever becomes `true` via `onVoiceAutoStart()` — itself only triggered `when route.studyMode == StudyMode.FAST`. Net effect: voice answering was reachable only in Fast mode and completely unreachable in Rated mode — the exact opposite of the intended use case (phone-in-pocket hands-free *rating*, which only makes sense where a rating step exists at all — Fast mode has none, per ADR-0016: "Fast mode has no Rating step... There is no Correct/Failed outcome per card").

Neither the design doc (`docs/design/premium-voice-grading-pipeline.md`) nor any prior ADR actually specified Fast vs. Rated scoping — both are silent on it. This was an implementation gap (the only existing "is a voice/TTS session running" flag in `StudySessionViewModel` happened to belong to Fast mode, so the implementing agent wired voice answering to it), not a documented decision this ADR is reversing. This ADR exists so that gap can't recur silently.

A second, related bug shared the same root cause: `VoiceAnswerController.start()` called `voiceCaptureEngine.startListening()` unconditionally with no coordination with any TTS playback state, so a phone playing its own TTS through a loudspeaker (no earphones) could have that audio picked up by the VAD as a spoken answer. Tying question-reading and answer-listening to one coordinated engine (this ADR's decision) is what actually closes that gap — the fix isn't a special case bolted onto the old wiring, it falls out of building the two capabilities on the same state machine from the start.

## Alternatives considered

**Keep voice answering attached to Fast mode, just fix the VAD/TTS overlap** — rejected. Fast mode structurally has no rating step and always auto-advances (question → pause → answer → next card, ADR-0012); there's no point in that flow for "wait for a spoken answer, then judge it" to occupy. The Fast-mode attachment wasn't a smaller-scope version of the right design, it was the wrong session type entirely.

**Build full mastery/XP/Terminal-state wiring in this same rework** — rejected. That system doesn't exist anywhere in the codebase yet (no rating enum, no Terminal-state write, no mastery/XP hookup for any session, manual or voice). Bolting a first-ever implementation of it onto a voice-pipeline correction risks scope explosion and building the persistence side without the UI/economy side (Home screen mastery display, XP totals) that would need to exist alongside it to mean anything. Tracked as a follow-up (see Consequences).

**Build a "TTS-reading-only, voice-answering off" middle Rated mode now** (question read aloud, user still manually reveals + self-rates) — rejected for the same reason: it needs the Failed/Partial/Correct self-rating buttons, which don't exist anywhere in this codebase regardless of TTS being involved. Building them just for this middle mode means half-building the deferred rating system. Tracked as a follow-up.

**A separate lightweight `TextToSpeech` instance for question-reading**, distinct from `VoiceGateway`/`TtsPlayer` (mirroring how `VoiceAnswerController` already handles its own grade/failure notices) — rejected once the shared-bottom-sheet requirement (speed slider, voice picker, play/pause, identical to Fast mode's) was set: that UI is hard-wired to `VoiceGateway`'s state (`isVoiceActive`/`isVoicePlaying` gate the whole `BottomSheetScaffold` in `StudySessionScreen.kt`), so a second TTS path would need a second bottom sheet. The grade/failure notice TTS stays a separate lightweight instance — it's a fire-and-forget announcement with no transport controls, genuinely different from question-reading.

**No silence timeout (listen indefinitely) or a re-prompt-before-skipping variant** — rejected for now in favor of a flat 8s timeout → audible skip. Indefinite listening risks a session hanging on one card with no recovery path in a mode that deliberately has no button fallback. Re-prompting once before skipping is a reasonable future refinement (see Consequences) but adds retry-count state this rework doesn't need yet.

**Grade as supplementary feedback only, manual rating buttons stay** — rejected. Phone-in-pocket use makes tapping a rating button impossible; if the feature's stated purpose is hands-free rating, the grade has to be the rating, not commentary alongside a UI action the user can't physically perform in this mode.

## Consequences

- `VoiceAnswerController`'s phase state machine needs new states/handling for: waiting-for-question-TTS-to-finish before listening starts, the 8s silence timeout, and pausing/ignoring capture during the grade-feedback notice.
- `VoiceGateway`/`TtsPlayer` needs a Rated-mode playback shape (stop after question, no auto-progress to answer) distinct from Fast mode's continuous auto-advance — a behavioral fork in what was previously a Fast-only engine.
- `StudySessionScreen.kt`'s `BottomSheetScaffold` content, currently gated only on `state.isVoiceActive`, needs to branch on Fast vs. Rated-voice-answering-on (different controls, different `sheetPeekHeight` semantics) — it can no longer assume "voice active" means "Fast mode."
- `functions/src/lib/grading.ts`'s Gemini prompt needs updating to: instruct inclusion of the full acceptable answer in feedback when the grade is Failed/Partial, and keep Correct feedback to a short affirmation.
- Grade-to-band mapping (Failed/Partial/Correct thresholds) needs to live somewhere shared enough that both the feedback-content logic and (later) the deferred rating-write logic can reuse it without drift.
- Explicit follow-ups, deferred out of this rework (same "itemize what's deferred" convention this feature already uses for its other blocked items):
  1. Wiring the auto-grade into actual Terminal-state/mastery/XP persistence (ADR-0016) — depends on that system being built at all, for any session type.
  2. The "TTS-reading-only, voice-answering off" middle Rated mode, once manual self-rating buttons exist.
  3. Silence-timeout handling upgraded from flat skip to "re-prompt the question once before skipping" (the rejected alternative above, explicitly kept as a future option).
  4. Surfacing the 8s silence timeout as a user-configurable preference in the Settings screen.
