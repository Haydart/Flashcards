# System TTS + MediaSessionCompat stack for Fast mode voice playback

## Decision

Fast Study Mode uses Android's system `TextToSpeech` engine for speech synthesis and `androidx.media` `MediaSessionCompat` + `NotificationCompat.MediaStyle` for lock-screen / notification transport controls. TTS is owned by a foreground `Service` (`StudySessionVoiceService`, `foregroundServiceType="mediaPlayback"`).

## Context

Fast mode requires: spoken question→pause→answer auto-advance, background/screen-off survival, persistent notification with prev/play-pause/next actions, and lock-screen controls identical to music players. Two candidate stacks were evaluated.

## Alternatives considered

**Media3 (ExoPlayer) with a TTS-backed player:**
The current recommended Android media stack. Would give `MediaSessionService` integration, system-managed session lifecycle, and seamless Bluetooth/wired headset handling out of the box. However, `TextToSpeech` is not a `Player` — bridging it requires wrapping each utterance as a `MediaItem` with a custom `MediaSource`, managing the TTS→audio track pipeline, or pre-synthesising to a file and feeding that to ExoPlayer. All paths add significant complexity for a feature that is inherently sequential utterances, not continuous audio streams.

**System TTS + MediaSessionCompat (chosen):**
`TextToSpeech` handles synthesis directly; `MediaSessionCompat` handles lock-screen transport and media-button events via `MediaButtonReceiver`; `NotificationCompat.MediaStyle` publishes the notification. The service drives playback via `UtteranceProgressListener` callbacks and a generation-counter guard to distinguish natural completion from interrupted utterances. Simpler, battle-tested on all API levels, and sufficient for the sequential utterance model Fast mode needs.

## Key rationale

- TTS is fundamentally utterance-based, not stream-based. Forcing it into a `Player` abstraction adds indirection with no gain for this use case.
- `MediaSessionCompat` covers the full lock-screen + Bluetooth + notification requirement without requiring Media3.
- If future Fast mode needs continuous background audio (e.g. ambient music mixing), the service can be migrated to Media3 at that point. The `LocalBinder` API surface is narrow enough that the ViewModel is insulated from the change.

## Consequences

- App must declare `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `POST_NOTIFICATIONS` permissions.
- TTS language is fixed to `Locale.US` (app is English-only); voice/language selection is deferred to a future Settings option.
- Speech rate is configurable (0.5×–2×) but restarts the current utterance on change (TTS cannot resume mid-word).
- Audio focus is managed manually (`AudioFocusRequest`): `AUDIOFOCUS_LOSS_TRANSIENT` pauses with auto-resume; `AUDIOFOCUS_LOSS` pauses without auto-resume.
- Backtick characters are stripped from spoken text at session load time (`forSpeech()` in `StudySessionVoiceGateway`) to prevent TTS from reading "backtick" aloud; Flashcard model text is unaffected.
