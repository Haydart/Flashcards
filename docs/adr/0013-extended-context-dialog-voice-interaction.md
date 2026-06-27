# Extended context as dialog with voice-aware pause/advance logic

## Decision

Extended context is displayed in a **popup dialog** (not inline). When the dialog is open during the between-card silence, the ViewModel silently pauses playback before TtsPlayer auto-advances. On dialog dismiss, if the pause was held by the dialog, playback auto-advances to the next card after a 500 ms linger. All hold/advance logic lives in the ViewModel; TtsPlayer receives only simple commands (play, pause, next).

## Context

Flashcards can have an `extendedContext` field with deeper explanation, code examples, and references. The original inline implementation forced the card into scroll mode and created UX problems when TTS was active:

- Re-entrancy ANR: calling `togglePlayPause()` synchronously inside a `StateFlow` collector triggered a re-entrant `publishState()` → `invalidateState()` → blocking Binder IPC on the main thread.
- Infinite toggle loop: after pausing, `isBetweenPause` remained `true` in the emitted state, causing the observer to fire `togglePlayPause` again on every state update.
- Scroll mode: inline extended context pushed card content off-screen, creating a jarring layout shift during voice study.

## Alternatives considered

**Keep inline, fix re-entrancy only:** Addressed the ANR but left scroll-mode disruption and the awkward "resume replays the answer you just heard" flow.

**Auto-resume on dialog dismiss (always):** Clean, but breaks the case where the user wants to review the visible answer and code blocks after closing the dialog before moving on.

**TtsPlayer holds advance internally (Option A in discussion):** TtsPlayer publishes a `WAITING_TO_ADVANCE` phase and waits for an external `commandAdvance()`. Cleaner signal boundary but bleeds UI concern (extended context state) into the player layer. Rejected: player should only receive simple commands.

**ViewModel rewinds after player advances (Option B):** Race condition — question may begin speaking before rewind fires, causing an audible glitch.

**Pause at between-pause start (chosen):** Between-card silence is inaudible, so pausing at `isInBetweenPause=true` is UX-invisible. ViewModel pauses via the existing `togglePlayPause` command and retains full ownership of advance logic.

## Interaction design

| Moment | Dialog closed | Dialog open |
|---|---|---|
| Answer speaking | plays normally | plays normally — no interruption |
| Between-pause silence starts (`isInBetweenPause=true`) | silence plays, auto-advances | ViewModel pauses immediately (silent) |
| Between-pause ends naturally | TtsPlayer advances to next card | n/a — already paused |
| Dialog dismissed | — | if paused by dialog: 500 ms linger → advance → auto-play next question |
| Dialog dismissed (user had manually paused first) | — | no auto-resume; playback stays paused |
| Forward/back tapped during 500 ms linger | cancels pending advance, honors tap | same |
| App backgrounded | player advances normally | no hold logic — tied to UI presence only |

## Key rationale

**Why dialog instead of inline:** Eliminates scroll-mode layout shift. Dialog dismiss is a clear user signal ("I'm done reading") that maps naturally to "continue playback."

**Why pause at silence start, not at advance time:** TtsPlayer's `advanceAfterCard()` fires from inside `onUtteranceDone` — there is no interception point available to the ViewModel without modifying player internals. Silence is inaudible, so pausing at its start is equivalent from the user's perspective.

**Why auto-advance on dismiss (not manual resume):** At dismiss time the user has heard the answer, the full silence, and read the extended context. Requiring a separate play tap adds friction with no benefit.

**Why 500 ms linger before advancing:** Gives the user a brief visual anchor on the current card before the next card appears, preventing a jarring instant cut.

**Why no auto-resume when user manually paused:** The dialog intercept is only meaningful when the pause was caused by the dialog. An explicit user pause is an explicit user intent; dialog dismiss must not override it.

**Why ViewModel owns all logic:** Extended context is a UI concern. TtsPlayer is unaware of it. Player stays testable in isolation with simple play/pause/next commands. If the app is backgrounded, no dialog exists and no hold ever fires — the player advances freely.

## ViewModel flag

`pausedDueToExtendedContext: Boolean` tracks whether the current pause was caused by the dialog intercept. Set to `true` when ViewModel issues the silent pause at `isInBetweenPause=true` while dialog is open. Cleared on card advance or on explicit user play/pause. Used to gate auto-advance on dialog dismiss.
