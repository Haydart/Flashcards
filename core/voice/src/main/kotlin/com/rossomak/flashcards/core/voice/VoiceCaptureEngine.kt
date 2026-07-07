package com.rossomak.flashcards.core.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
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
import javax.inject.Inject
import javax.inject.Singleton

/** A single VAD-bounded utterance. Only obfuscated audio ever leaves the engine in this form. */
data class CapturedUtterance(
    val obfuscatedPcm: ShortArray,
    val wavBytes: ByteArray,
    val durationMs: Long,
)

sealed interface VoiceCaptureEvent {
    data object SpeechStarted : VoiceCaptureEvent
    data object SpeechEnded : VoiceCaptureEvent
    data class UtteranceCaptured(val utterance: CapturedUtterance) : VoiceCaptureEvent
    data class CaptureFailed(val reason: String) : VoiceCaptureEvent
}

/**
 * Continuous mic capture: AudioRecord (VOICE_RECOGNITION source, 16kHz mono PCM) feeding 20ms
 * frames through the [VoiceActivityDetector]; VAD-bounded utterances are buffered, run through
 * the [VoiceObfuscator] on-device, WAV-wrapped and emitted via [events]. Raw (un-obfuscated)
 * buffers are zeroed as soon as the obfuscated copy exists.
 *
 * Bluetooth earphone mics are routed via SCO ([AudioManager.startBluetoothSco] pre-S,
 * `setCommunicationDevice` on S+) — the only reliable path for BT Classic headsets.
 *
 * Callers own the surrounding foreground-service + wake-lock lifecycle (feature:study).
 */
@Singleton
class VoiceCaptureEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceActivityDetector: VoiceActivityDetector,
    private val voiceObfuscator: VoiceObfuscator,
) {

    private val _events = MutableSharedFlow<VoiceCaptureEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<VoiceCaptureEvent> = _events.asSharedFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var captureJob: Job? = null
    private val audioManager: AudioManager
        get() = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
        stopBluetoothScoRouting()
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
        val audioRecord = createAudioRecord() ?: return@withContext ShortArray(0)
        startBluetoothScoRoutingIfNeeded()
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
            stopBluetoothScoRouting()
        }
        clip
    }

    fun obfuscate(pcm: ShortArray): ShortArray = voiceObfuscator.obfuscate(pcm)

    fun rerandomizeObfuscation() = voiceObfuscator.randomizeSessionShift()

    fun encodeWav(pcm: ShortArray): ByteArray = WavEncoder.encode(pcm, SAMPLE_RATE_HZ)

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private suspend fun runCaptureLoop() {
        val audioRecord = createAudioRecord() ?: run {
            _events.emit(VoiceCaptureEvent.CaptureFailed("AudioRecord initialization failed"))
            _isListening.value = false
            return
        }
        startBluetoothScoRoutingIfNeeded()
        val frame = ShortArray(FRAME_SIZE_SAMPLES)
        val preRoll = ArrayDeque<ShortArray>(PRE_ROLL_FRAMES)
        val utterance = mutableListOf<ShortArray>()
        var isInUtterance = false
        var trailingSilenceFrames = 0
        try {
            audioRecord.startRecording()
            while (captureJob?.isActive == true) {
                val read = audioRecord.read(frame, 0, frame.size)
                if (read <= 0) continue
                val isSpeech = voiceActivityDetector.isSpeech(frame)
                _isSpeechDetected.value = isSpeech
                when {
                    isSpeech && !isInUtterance -> {
                        isInUtterance = true
                        trailingSilenceFrames = 0
                        utterance.clear()
                        utterance.addAll(preRoll)
                        preRoll.clear()
                        utterance.add(frame.copyOf())
                        _events.emit(VoiceCaptureEvent.SpeechStarted)
                    }
                    isInUtterance -> {
                        utterance.add(frame.copyOf())
                        trailingSilenceFrames = if (isSpeech) 0 else trailingSilenceFrames + 1
                        val utteranceEnded = trailingSilenceFrames >= END_SILENCE_FRAMES
                        val utteranceTooLong = utterance.size >= MAX_UTTERANCE_FRAMES
                        if (utteranceEnded || utteranceTooLong) {
                            isInUtterance = false
                            _events.emit(VoiceCaptureEvent.SpeechEnded)
                            finishUtterance(utterance, trailingSilenceFrames)
                            utterance.clear()
                            trailingSilenceFrames = 0
                        }
                    }
                    else -> {
                        preRoll.addLast(frame.copyOf())
                        if (preRoll.size > PRE_ROLL_FRAMES) preRoll.removeFirst()
                    }
                }
            }
        } catch (exception: SecurityException) {
            _events.emit(VoiceCaptureEvent.CaptureFailed("Microphone permission missing: ${exception.message}"))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            // VAD inference or AudioRecord failures used to kill the loop silently, leaving the UI
            // stuck on "silence". Surface them so they show up in the debug event log.
            android.util.Log.e(TAG, "capture loop error", exception)
            _events.emit(VoiceCaptureEvent.CaptureFailed("Capture loop error: ${exception.message}"))
        } finally {
            runCatching { audioRecord.stop() }
            audioRecord.release()
            _isSpeechDetected.value = false
        }
    }

    private suspend fun finishUtterance(frames: List<ShortArray>, trailingSilenceFrames: Int) {
        val speechFrameCount = frames.size - trailingSilenceFrames
        if (speechFrameCount < MIN_UTTERANCE_FRAMES) return
        val raw = ShortArray(frames.size * FRAME_SIZE_SAMPLES)
        frames.forEachIndexed { index, chunk -> chunk.copyInto(raw, index * FRAME_SIZE_SAMPLES) }
        val obfuscated = voiceObfuscator.obfuscate(raw)
        raw.fill(0) // Raw voiceprint is dropped the moment the obfuscated copy exists.
        val durationMs = obfuscated.size * 1000L / SAMPLE_RATE_HZ
        _events.emit(
            VoiceCaptureEvent.UtteranceCaptured(
                CapturedUtterance(
                    obfuscatedPcm = obfuscated,
                    wavBytes = WavEncoder.encode(obfuscated, SAMPLE_RATE_HZ),
                    durationMs = durationMs,
                )
            )
        )
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return null
        val audioRecord = AudioRecord(
            preferredAudioSource(),
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, FRAME_SIZE_SAMPLES * Short.SIZE_BYTES * BUFFER_FRAMES),
        )
        return if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord
        } else {
            audioRecord.release()
            null
        }
    }

    /**
     * VOICE_RECOGNITION routes through OEM noise-suppression/AGC that gutted the signal to near
     * silence on some devices (e.g. Realme/MTK). Prefer UNPROCESSED — a raw, effect-free mic feed,
     * ideal for our own VAD + cloud ASR — when the device advertises support, otherwise fall back to
     * the plain MIC source, which still carries a usable level.
     */
    private fun preferredAudioSource(): Int = MediaRecorder.AudioSource.MIC

    @SuppressLint("DEPRECATION") // startBluetoothSco is the only path below API 31.
    private fun startBluetoothScoRoutingIfNeeded() {
        val manager = audioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scoDevice = manager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            scoDevice?.let { manager.setCommunicationDevice(it) }
        } else if (manager.isBluetoothScoAvailableOffCall) {
            manager.startBluetoothSco()
        }
    }

    @SuppressLint("DEPRECATION")
    private fun stopBluetoothScoRouting() {
        val manager = audioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.clearCommunicationDevice()
        } else {
            runCatching { manager.stopBluetoothSco() }
        }
    }

    companion object {
        private const val TAG = "VoiceCapture"
        const val SAMPLE_RATE_HZ = 16_000
        const val FRAME_DURATION_MS = 20
        const val FRAME_SIZE_SAMPLES = SAMPLE_RATE_HZ * FRAME_DURATION_MS / 1000
        private const val BUFFER_FRAMES = 8

        private const val PRE_ROLL_FRAMES = 10 // 200ms of audio kept before speech onset
        // 1.5s silence closes the utterance. Deliberately generous: spoken answers contain
        // thinking-pauses, and a shorter window cut recordings off mid-answer. Silero's accurate
        // soft-speech detection keeps these pauses from being padded with false-positive frames.
        private const val END_SILENCE_FRAMES = 75
        private const val MIN_UTTERANCE_FRAMES = 15 // <300ms of speech is discarded as noise
        private const val MAX_UTTERANCE_FRAMES = 750 // 15s hard cap per utterance
    }
}
