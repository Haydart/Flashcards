# Real-Time Audio Spectrum Indicator for Voice Answering

Design overview for adding a live, mic-driven equalizer/spectrum visualization
to the voice-answering UI in the study session screen. This document captures
the architecture and rationale so it can be picked up and implemented as a
self-contained follow-up to the [voice answering pipeline](../adr/0026-voice-grade-unifies-with-rating-reveal-tied-to-speech-end.md)
work. It intentionally avoids line-number references and literal code, since
the underlying files are expected to have moved on by the time this is
implemented — it describes the shape of the change, not a diff.

## Problem

`StudySessionScreen`'s voice-answer bottom sheet currently gives the user no
visual feedback that the microphone is actually picking up sound. While
`isVoiceAnswerEnabled` is on, the UI shows only a static mic icon and a text
label ("Listening…" / "Speech detected" etc., driven by `VoiceAnswerPhase`).
There is no indication of input level, so a user can't tell at a glance
whether the mic is live, whether they're too quiet, or whether anything is
being picked up at all.

The ask: a real-time equalizer-style visualization — a centered mic icon with
animated spectrum bars fanning out to its left and right, reacting to the
live audio input — similar in spirit to voice-message recording UIs
(WhatsApp, Siri).

## Current state

Nothing in the capture pipeline computes any amplitude, level, or spectrum
data today. The only signal derived from the raw audio is
`SileroVoiceActivityDetector.speechProbability`, a scalar 0–1 confidence
value from the Silero VAD model, currently wired only into the debug voice
screen as plain monospace text (no graphical visualization anywhere in the
app, no chart/FFT library in the dependency graph).

The relevant pieces of the existing pipeline:

- **`core:voice` module** — `VoiceCaptureEngine` owns a background coroutine
  that continuously reads 20ms/320-sample PCM frames from `AudioRecord` (16kHz
  mono) and feeds each frame to `VoiceActivityDetector.isSpeech(frame)`.
  `SileroVoiceActivityDetector` (the production implementation) internally
  batches those 320-sample frames into 512-sample windows before running
  Silero ONNX inference, since the model requires exactly 512 samples per
  call.
- **State relay** — `VoiceCaptureEngine` emits coarse events
  (`SpeechStarted`/`SpeechEnded`/etc.) and state (`isSpeechDetected`,
  `isListening`) that `VoiceAnswerController` (in `feature/study`) consumes
  and turns into a `VoiceAnswerState` (an enum-driven `VoiceAnswerPhase` plus
  grade/error fields). This state crosses a same-process `Binder` boundary
  (`StudySessionVoiceService` → `StudySessionVoiceGateway`) — a plain
  in-memory `StateFlow` reference hand-off, not real IPC/serialization — then
  flows into `StudySessionViewModel`, which maps it into
  `StudySessionScreenState` for the Compose UI to observe.
- **UI** — `StudySessionScreen`'s bottom sheet content renders the phase
  label and a small `Mic`/`MicOff` `IconButton` that toggles voice answering.

This existing phase-relay path is the one to reuse: it's the exact plumbing
pattern a new spectrum signal should ride along, rather than inventing a
separate channel.

## Feasibility: is a real FFT spectrum cheap enough?

Yes. `SileroVoiceActivityDetector` already accumulates raw frames into
512-sample windows on the same background coroutine that reads from
`AudioRecord`, at roughly a 32ms cadence (512 samples / 16kHz). A radix-2
FFT over a 512-sample window is O(n log n) ≈ 512 × 9 ≈ 4,600 operations —
sub-millisecond on any phone CPU, negligible next to the ONNX inference call
already happening at the same cadence on that same thread. There's no
existing FFT implementation or library dependency in the repo, so this needs
a small hand-rolled FFT (radix-2 Cooley-Tukey is well within reach at ~60-80
lines, no new Gradle dependency required), but the added *latency* is a
non-issue. This makes a real (if coarse) frequency-domain spectrum
preferable to faking bar movement with randomized multipliers off a single
scalar amplitude value — a real spectrum will look authentic instead of
synthetic.

## Proposed design

### 1. Spectrum analysis (`core:voice`)

A new component, analogous in role to `SileroVoiceActivityDetector` but
independent from it (keep VAD and spectrum-visualization concerns decoupled
— they consume the same raw frames but serve different purposes and
shouldn't share internal buffering state):

- Accumulates raw 320-sample frames into its own 512-sample window buffer.
- On each completed window: applies a Hann window function (reduces spectral
  leakage), runs the FFT, computes per-bin magnitude, and groups the result
  into roughly a dozen log-spaced frequency bands. Log spacing matters here —
  most speech energy sits below ~4kHz, so linear bin spacing over the full
  8kHz Nyquist range would waste most of the visual bands on frequencies
  speech barely touches.
- Normalizes each band to a 0–1 range using a fixed dB floor/ceiling (e.g.
  -60dB to 0dB, clamped) rather than a running-max AGC scheme — predictable,
  stateless behavior, consistent with the fixed-threshold style the VAD
  already uses (`SPEECH_THRESHOLD`).
- Applies a per-band attack/release envelope (fast rise, slower decay) so the
  bars read as smooth motion rather than jittering frame-to-frame. This also
  effectively caps the perceptible update rate without needing to throttle
  emission separately.
- Exposes the current band levels as a `StateFlow<List<Float>>` on
  `VoiceCaptureEngine`, populated from the same point in the capture loop
  where frames are currently handed to the VAD — same raw `ShortArray`, no
  extra copying.
- Gets reset alongside the VAD's reset whenever a new listening session
  starts, so stale band levels don't leak into a fresh utterance.

### 2. Relay through the existing state chain

The new band-level list should be threaded through exactly the same path
`voiceAnswerPhase` already takes today, as one more field:

`VoiceCaptureEngine.spectrumLevels` → `VoiceAnswerController` mirrors it into
`VoiceAnswerState` → rides across the existing same-process service binder
unchanged (no binder-layer changes needed, since the whole state object is
already relayed as-is) → `StudySessionViewModel` maps it into
`StudySessionScreenState` → observed by the Compose UI.

No new transport mechanism, no new binder methods, no gating logic beyond
what already exists — the engine only produces frames while its own capture
loop is active, so downstream layers can simply forward whatever they
receive.

### 3. UI — mic icon with fanning spectrum bars

Replace the current standalone mic `IconButton` with a composite element:
a row of bars representing the lower-half of the band list fanning outward
to the left, the mic icon (still the same tap target, same toggle behavior)
centered, and the upper-half of the band list fanning outward to the right.

- Each bar is a small rounded shape whose height animates toward its target
  band level (`animateFloatAsState` or an `Animatable` per bar — this would
  be the first use of Compose's animation APIs anywhere in this app beyond a
  one-off splash-screen effect, so the spring/tween spec used here is worth
  treating as a small reusable local constant rather than inlining magic
  numbers per bar).
- Bar color reuses the existing primary-color accent already used to
  highlight the `SPEECH_DETECTED` phase label, keeping the new element
  visually consistent with the rest of the voice-answer UI rather than
  introducing a new palette token.
- When voice answering is toggled off, the element should fall back to
  exactly today's plain static mic icon — bars only appear once voice
  answering is enabled, so the disabled state is visually unchanged.
- Layout should stay within the existing row's height — this is additive
  visual richness to a UI element, not a new section of the sheet.

## Open items for the implementer

- Exact band count (12 is a reasonable starting point — enough to look like
  a real spectrum, few enough to stay cheap to animate) and left/right index
  mapping (mirrored vs. split-by-frequency) should be tuned by eye once
  something is on-device; treat the numbers above as a starting point, not a
  spec.
- Watch Compose recomposition cost with ~12+ animated bars updating at
  roughly 15–30Hz during active listening. If this turns out to be heavier
  than expected, prefer coarsening the analyzer's own envelope/update cadence
  first over throttling on the UI side — cheaper to slow down the source than
  to add frame-dropping logic in the composable.
- Unit-testing the analyzer against synthetic sine-wave input (known
  frequency in, expect energy in the matching band; silence in, expect
  near-zero across all bands) is straightforward and should be the primary
  correctness check before relying on by-eye tuning on a physical device.
