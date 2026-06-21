# Voice Mode MVP — Study Session

## Goal

System TTS reads flashcard question, pauses, reads answer, advances to next card. Media-player-style controls in a persistent notification and on the study screen.

## Entry point

Voice mode is **not a separate route or Pre-start mode** (neither exists yet). It is a toggle in
the `StudySessionScreen` `TopAppBar` (`RecordVoiceOver` icon) that layers playback onto the active
session — so it works standalone or as read-along while the user looks at the card. Turning it off
(or exiting the session) stops playback and the service.

## Stack

- **`TextToSpeech`** — Android system TTS, no extra dependency
- **`ForegroundService`** (plain, not Media3) — `StudySessionVoiceService`; owns TTS lifecycle, posts `MediaStyle` notification, `foregroundServiceType="mediaPlayback"`
- **`androidx.media` `MediaSessionCompat` + `MediaStyle` notification** — play/pause, skip-next, skip-prev actions; works on lock screen and with bluetooth/wired media buttons (`MediaButtonReceiver`)
- ViewModel binds to service via `LocalBinder`, observes `StateFlow<VoicePlaybackState>`; controls live in a persistent `BottomSheetScaffold`

## Service: `StudySessionVoiceService`

Owns:
- `TextToSpeech` instance (initialized on `onCreate`)
- Card queue + current index
- Playback phase: `QUESTION | PAUSE | ANSWER | BETWEEN_CARDS`

Sequence per card:
1. `speak(question, QUEUE_FLUSH, utteranceId="question")`
2. `onDone("question")` → `playSilentUtterance(PAUSE_MS)` 
3. `onDone("pause")` → `speak(answer, QUEUE_FLUSH, utteranceId="answer")`
4. `onDone("answer")` → `playSilentUtterance(BETWEEN_CARDS_MS)` → advance index → repeat

Stale-callback guard: every utterance id embeds a `generation` counter, bumped on each new
utterance and each interrupting command. An `onDone` whose generation no longer matches the latest
is ignored — this distinguishes a naturally finished utterance from one cancelled by `tts.stop()`.

Controls:
- `togglePlayPause()` — `pause()` = `tts.stop()` (keeps phase+index); `play()` = re-speak current phase from start
- `skipNext()` / `skipPrevious()` — move card, restart at question (speaks only if playing)
- `showAnswer()` — interrupts the question and immediately speaks the answer (phase → ANSWER)
- `setSpeechRate(rate)` — `tts.setSpeechRate`; restarts the current utterance for an immediate effect
- `stopPlayback()` — `tts.stop()`, `stopForeground`, `stopSelf`

Notification:
- `MediaStyle` with 3 compact actions: skip-prev, play/pause, skip-next
- Content title: subcategory name; text: `"${index+1} / ${total}"`
- Updated on every phase/index change; `PlaybackStateCompat` kept in sync for lock-screen controls

## ViewModel changes

- Binds/starts the service on voice toggle (`startForegroundService` + `bindService`), unbinds on toggle-off and `onCleared`
- On connect, pushes the resolved card queue (`questionSpoken`/`answerSpoken` with fallback to `question`/`answer`) via `binder.loadSession(cards, startIndex, subcategoryName)`
- Merges `binder.state: StateFlow<VoicePlaybackState>` into screen state: when active, `currentCardIndex` follows the service and `isAnswerRevealed = phase == ANSWER`
- Delegates `onVoicePlayPause / onVoiceNext / onVoicePrevious / onVoiceSpeedChange`; `onShowAnswer` routes to `binder.showAnswer()` when active

## UI changes (`StudySessionScreen`)

- `Scaffold` → persistent `BottomSheetScaffold` (no swipe, fixed peek) hosting the controls
- Voice toggle button in `TopAppBar` actions (`RecordVoiceOver`, tinted when active)
- Voice active sheet: Show Answer (during question) + transport row (prev, play/pause, next) + speed `Slider`
- Voice inactive sheet: existing Show Answer → Next/Finish buttons
- Card body stays visible (read-along); answer reveal is state-driven and tracks playback
- `POST_NOTIFICATIONS` requested on first toggle-on (API 33+); playback degrades gracefully if denied

## Timing defaults

| Gap | Duration |
|-----|----------|
| Question → Answer | 1 500 ms |
| Answer → Next card | 2 500 ms |

## Implemented (was previously out of scope)

- Playback speed control (screen-only `Slider`)
- Lock-screen + bluetooth/wired media-button controls (`MediaSessionCompat` + `MediaButtonReceiver`)
- Background / screen-off playback (foreground `mediaPlayback` service)

## Out of scope

- TTS language/voice selection
- Lock-screen artwork
- Background-only mode (no UI / no session screen)
- Fast-session metadata write to Firestore (`recentSessions`, `studyMode="fast"`) — deferred to a separate change
