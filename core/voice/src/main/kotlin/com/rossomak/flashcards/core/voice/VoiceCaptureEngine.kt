package com.rossomak.flashcards.core.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A single VAD-bounded utterance. Only obfuscated audio ever leaves the engine in this form. */
data class CapturedUtterance(val obfuscatedPcm: ShortArray, val wavBytes: ByteArray, val durationMs: Long)

sealed interface VoiceCaptureEvent {
    data object SpeechStarted : VoiceCaptureEvent
    data object SpeechEnded : VoiceCaptureEvent
    data class UtteranceCaptured(val utterance: CapturedUtterance) : VoiceCaptureEvent
    data class CaptureFailed(val reason: String) : VoiceCaptureEvent
}

/**
 * Continuous mic capture: AudioRecord (16kHz mono PCM) feeding 20ms frames through the
 * [VoiceActivityDetector]; VAD-bounded utterances are buffered, run through the [VoiceObfuscator]
 * on-device, WAV-wrapped and emitted via [events]. Raw (un-obfuscated) buffers are zeroed as soon
 * as the obfuscated copy exists.
 *
 * Microphone *routing* (BLE-first, SCO fallback, Bluetooth-strict — ADR-0027) is owned by
 * [AudioRouteManager] at the session level, not here. This engine only reads the currently active
 * [CaptureRoute]: it binds the input device via `AudioRecord.setPreferredDevice`, verifies the
 * honored [AudioRecord.getRoutedDevice] after `startRecording()`, and — when a Bluetooth mic is
 * required but unavailable ([CaptureRouteType.WAITING]) — strict-pauses rather than silently falling
 * back to the pocketed phone mic. On a mid-session route change it rebuilds the [AudioRecord] at the
 * next utterance boundary (an [AudioRecord] cannot change device live).
 *
 * Callers own the surrounding foreground-service + wake-lock lifecycle and the session route
 * (feature:study): they call [AudioRouteManager.acquireSessionRoute] before listening and
 * [AudioRouteManager.releaseSessionRoute] at session end.
 */
@Singleton
class VoiceCaptureEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceActivityDetector: VoiceActivityDetector,
    private val voiceObfuscator: VoiceObfuscator,
    private val audioRouteManager: AudioRouteManager
) {

    private enum class CaptureOutcome { STOPPED, FAILED, ROUTE_CHANGED }

    private val _events = MutableSharedFlow<VoiceCaptureEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<VoiceCaptureEvent> = _events.asSharedFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var captureJob: Job? = null

    /** Starts continuous VAD-driven capture. No-op when already listening. */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (captureJob?.isActive == true) return
        voiceActivityDetector.reset()
        voiceObfuscator.randomizeSessionShift()
        _isListening.value = true
        captureJob = scope.launch { runCaptureLoop() }
    }

    fun stopListening() {
        captureJob?.cancel()
        captureJob = null
        _isListening.value = false
        _isSpeechDetected.value = false
    }

    /**
     * One-shot fixed-length recording of *raw* (un-obfuscated) PCM. Debug-screen use only —
     * on-device playback for verifying capture and judging the obfuscation A/B by ear. This
     * audio must never be uploaded; production capture goes through [startListening], which
     * emits obfuscated audio exclusively.
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    suspend fun recordRawClip(durationMs: Long): ShortArray = withContext(Dispatchers.IO) {
        val totalSamples = (SAMPLE_RATE_HZ * durationMs / 1000).toInt()
        val clip = ShortArray(totalSamples)
        // Debug capture: use whatever route is active (phone if no session route acquired); no
        // strict-pause or verification here — this path is dev-only, never production capture.
        val audioRecord = createAudioRecord(audioRouteManager.route.value) ?: return@withContext ShortArray(0)
        try {
            audioRecord.startRecording()
            var readSamples = 0
            while (readSamples < totalSamples) {
                val read = audioRecord.read(clip, readSamples, totalSamples - readSamples)
                if (read <= 0) break
                readSamples += read
            }
        } finally {
            runCatching { audioRecord.stop() }
            audioRecord.release()
        }
        clip
    }

    fun obfuscate(pcm: ShortArray): ShortArray = voiceObfuscator.obfuscate(pcm)

    fun rerandomizeObfuscation() = voiceObfuscator.randomizeSessionShift()

    fun encodeWav(pcm: ShortArray): ByteArray = WavEncoder.encode(pcm, SAMPLE_RATE_HZ)

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private suspend fun runCaptureLoop() {
        // A route change (BT connect/disconnect mid-session) can't be applied to a live AudioRecord,
        // so it's applied by rebuilding at the next utterance boundary. This flag is raised by the
        // route-change collector and consumed at a safe point inside captureFrames().
        var routeChangePending = false
        val routeChangeJob = scope.launch {
            audioRouteManager.routeChanges.collect { routeChangePending = true }
        }
        try {
            while (captureJob?.isActive == true) {
                val route = audioRouteManager.route.value
                if (!route.isCapturable) {
                    // Bluetooth-strict (ADR-0027): a mic-capable BT device is connected but its link
                    // isn't ready — never fall back to the pocketed phone mic. Strict-pause instead.
                    _events.emit(VoiceCaptureEvent.CaptureFailed("Bluetooth microphone unavailable"))
                    return
                }
                routeChangePending = false
                val audioRecord = createAudioRecord(route) ?: run {
                    _events.emit(VoiceCaptureEvent.CaptureFailed("AudioRecord initialization failed"))
                    return
                }
                val outcome = try {
                    audioRecord.startRecording()
                    if (!isRouteHonored(audioRecord, route)) {
                        _events.emit(VoiceCaptureEvent.CaptureFailed("Capture not routed to Bluetooth microphone"))
                        CaptureOutcome.FAILED
                    } else {
                        captureFrames(audioRecord) { routeChangePending }
                    }
                } catch (exception: SecurityException) {
                    _events.emit(VoiceCaptureEvent.CaptureFailed("Microphone permission missing: ${exception.message}"))
                    CaptureOutcome.FAILED
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    // VAD inference or AudioRecord failures used to kill the loop silently, leaving
                    // the UI stuck on "silence". Surface them so they show up in the debug event log.
                    android.util.Log.e(TAG, "capture loop error", exception)
                    _events.emit(VoiceCaptureEvent.CaptureFailed("Capture loop error: ${exception.message}"))
                    CaptureOutcome.FAILED
                } finally {
                    runCatching { audioRecord.stop() }
                    audioRecord.release()
                }
                when (outcome) {
                    CaptureOutcome.STOPPED, CaptureOutcome.FAILED -> return
                    CaptureOutcome.ROUTE_CHANGED -> Unit // loop and rebuild AudioRecord on the new route
                }
            }
        } finally {
            routeChangeJob.cancel()
            _isListening.value = false
            _isSpeechDetected.value = false
        }
    }

    /**
     * Reads VAD-bounded utterances off [audioRecord] until the job is cancelled ([CaptureOutcome.STOPPED])
     * or [isRouteChangePending] flips. A pending route change is honored at an utterance boundary:
     * an in-flight utterance is finished first, then [CaptureOutcome.ROUTE_CHANGED] is returned so the
     * caller rebuilds on the new route (never a mid-clip device switch).
     */
    private suspend fun captureFrames(audioRecord: AudioRecord, isRouteChangePending: () -> Boolean): CaptureOutcome {
        val frame = ShortArray(FRAME_SIZE_SAMPLES)
        val preRoll = ArrayDeque<ShortArray>(PRE_ROLL_FRAMES)
        val utterance = mutableListOf<ShortArray>()
        var isInUtterance = false
        var trailingSilenceFrames = 0
        var speechFrameCount = 0
        while (captureJob?.isActive == true) {
            if (isRouteChangePending() && !isInUtterance) return CaptureOutcome.ROUTE_CHANGED
            val read = audioRecord.read(frame, 0, frame.size)
            if (read <= 0) continue
            if (read < frame.size) frame.fill(0, read, frame.size)
            val isSpeech = voiceActivityDetector.isSpeech(frame)
            _isSpeechDetected.value = isSpeech
            when {
                isSpeech && !isInUtterance -> {
                    isInUtterance = true
                    trailingSilenceFrames = 0
                    speechFrameCount = 1
                    utterance.clear()
                    utterance.addAll(preRoll)
                    preRoll.clear()
                    utterance.add(frame.copyOf())
                    _events.emit(VoiceCaptureEvent.SpeechStarted)
                }
                isInUtterance && isSpeech -> {
                    utterance.add(frame.copyOf())
                    speechFrameCount++
                    trailingSilenceFrames = 0
                }
                isInUtterance -> {
                    // Only pad a short context window (leading-in for the next burst, or
                    // trailing-out for this one) into the buffer; frames beyond GAP_PAD_FRAMES
                    // are neither appended nor obfuscated/uploaded — dead air between
                    // thinking-pauses never leaves the device. trailingSilenceFrames still
                    // counts every silent frame so the END_SILENCE_FRAMES hangover timing is
                    // unaffected.
                    trailingSilenceFrames++
                    if (trailingSilenceFrames <= GAP_PAD_FRAMES) {
                        utterance.add(frame.copyOf())
                    }
                    val utteranceEnded = trailingSilenceFrames >= END_SILENCE_FRAMES
                    val utteranceTooLong = utterance.size >= MAX_UTTERANCE_FRAMES
                    if (utteranceEnded || utteranceTooLong) {
                        isInUtterance = false
                        _events.emit(VoiceCaptureEvent.SpeechEnded)
                        finishUtterance(utterance, speechFrameCount)
                        utterance.clear()
                        trailingSilenceFrames = 0
                        speechFrameCount = 0
                        // Finished the in-flight utterance; now it's safe to switch devices.
                        if (isRouteChangePending()) return CaptureOutcome.ROUTE_CHANGED
                    }
                }
                else -> {
                    preRoll.addLast(frame.copyOf())
                    if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                }
            }
        }
        return CaptureOutcome.STOPPED
    }

    private suspend fun finishUtterance(frames: List<ShortArray>, speechFrameCount: Int) {
        if (speechFrameCount < MIN_UTTERANCE_FRAMES) return
        val raw = ShortArray(frames.size * FRAME_SIZE_SAMPLES)
        frames.forEachIndexed { index, chunk -> chunk.copyInto(raw, index * FRAME_SIZE_SAMPLES) }
        frames.forEach { it.fill(0) } // Per-frame copies are scrubbed once folded into raw.
        val obfuscated = voiceObfuscator.obfuscate(raw)
        raw.fill(0) // Raw voiceprint is dropped the moment the obfuscated copy exists.
        val durationMs = obfuscated.size * 1000L / SAMPLE_RATE_HZ
        _events.emit(
            VoiceCaptureEvent.UtteranceCaptured(
                CapturedUtterance(
                    obfuscatedPcm = obfuscated,
                    wavBytes = WavEncoder.encode(obfuscated, SAMPLE_RATE_HZ),
                    durationMs = durationMs
                )
            )
        )
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(route: CaptureRoute): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return null
        val audioRecord = AudioRecord(
            preferredAudioSource(),
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, FRAME_SIZE_SAMPLES * Short.SIZE_BYTES * BUFFER_FRAMES)
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return null
        }
        // Bind the input to the Bluetooth mic before recording starts — OEMs latch the input route
        // at startRecording(), so the preferred device must be set here, not after.
        route.device?.let { audioRecord.setPreferredDevice(it) }
        return audioRecord
    }

    /**
     * After startRecording(), confirm a Bluetooth route actually landed on the Bluetooth mic. A
     * mismatch means the OEM silently steered capture to the phone mic — which, phone-in-pocket, is
     * the core failure this pipeline exists to prevent. Phone/none routes are trivially honored.
     */
    private fun isRouteHonored(audioRecord: AudioRecord, route: CaptureRoute): Boolean {
        if (!route.isBluetooth) return true
        val routedType = audioRecord.routedDevice?.type ?: return false
        return routedType == route.device?.type
    }

    /**
     * Single swappable audio-source line (ADR-0027 Q1), pending the on-device A/B test. Kept as MIC:
     * VOICE_RECOGNITION routed through OEM noise-suppression/AGC that gutted the signal to near
     * silence on some devices (e.g. Realme/MTK).
     */
    private fun preferredAudioSource(): Int = MediaRecorder.AudioSource.MIC

    companion object {
        private const val TAG = "VoiceCapture"
        const val SAMPLE_RATE_HZ = 16_000
        const val FRAME_DURATION_MS = 20
        const val FRAME_SIZE_SAMPLES = SAMPLE_RATE_HZ * FRAME_DURATION_MS / 1000
        private const val BUFFER_FRAMES = 8

        private const val PRE_ROLL_FRAMES = 10 // 200ms of audio kept before speech onset

        // 2.5s silence closes the utterance. Deliberately generous: spoken answers contain
        // thinking-pauses, and a shorter window cut recordings off mid-answer. Silero's accurate
        // soft-speech detection keeps these pauses from being padded with false-positive frames.
        private const val END_SILENCE_FRAMES = 125

        // Only this much silence is kept around each speech burst (leading-in for the next one,
        // trailing-out for this one) before ElevenLabs upload; the rest of a thinking-pause is
        // trimmed at capture time. Silence carries no ASR signal, so cutting it is safe and keeps
        // payload/latency down — the pad is just insurance against clipping a boundary word.
        private const val GAP_PAD_FRAMES = 8 // 160ms
        private const val MIN_UTTERANCE_FRAMES = 15 // <300ms of speech is discarded as noise
        private const val MAX_UTTERANCE_FRAMES = 1500 // 30s hard cap per utterance
    }
}
