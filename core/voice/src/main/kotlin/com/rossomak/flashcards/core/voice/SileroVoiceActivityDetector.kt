package com.rossomak.flashcards.core.voice

import com.rossomak.flashcards.core.voice.SileroVadSession.Companion.WINDOW_SIZE_SAMPLES
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Neural VAD backed by the Silero v5 ONNX model — far more robust than the previous energy-based
 * detector at recognising soft or breathy speech, which is what caused that detector to mis-read
 * quiet mid-sentence audio as silence and cut recordings short.
 *
 * The model needs exactly [WINDOW_SIZE_SAMPLES] (512) samples per inference, but [VoiceCaptureEngine]
 * delivers 320-sample (20ms) frames. This detector bridges the mismatch by accumulating incoming
 * frames and running inference on whole 512-sample hops, carrying leftover samples ([carry]) into
 * the next call so the model always sees contiguous, non-overlapping windows (required for its
 * recurrent state to stay coherent). Frames that don't complete a new window return the last
 * computed classification rather than flipping to silence — so a partial low-energy frame never on
 * its own ends an utterance; utterance segmentation (the trailing-silence hangover) stays the
 * engine's job.
 */
@Singleton
class SileroVoiceActivityDetector @Inject constructor(private val session: SileroVadSession) : VoiceActivityDetector {

    private var carry = ShortArray(0)
    private var lastSpeechProbability = 0f

    private val _speechProbability = MutableStateFlow(0f)

    /** Last model probability, for the debug Voice screen to visualise/tune the VAD. */
    val speechProbability: StateFlow<Float> = _speechProbability.asStateFlow()

    override fun isSpeech(frame: ShortArray): Boolean {
        val buffer = if (carry.isEmpty()) frame else carry + frame
        var offset = 0
        while (buffer.size - offset >= WINDOW_SIZE_SAMPLES) {
            lastSpeechProbability = session.run(buffer.normalisedWindow(offset))
            offset += WINDOW_SIZE_SAMPLES
        }
        carry = if (offset == 0) buffer.copyOf() else buffer.copyOfRange(offset, buffer.size)
        _speechProbability.value = lastSpeechProbability
        return lastSpeechProbability >= SPEECH_THRESHOLD
    }

    override fun reset() {
        carry = ShortArray(0)
        lastSpeechProbability = 0f
        _speechProbability.value = 0f
        session.reset()
    }

    private fun ShortArray.normalisedWindow(from: Int): FloatArray =
        FloatArray(WINDOW_SIZE_SAMPLES) { index -> this[from + index] / PCM_16BIT_MAX }

    private companion object {
        // Silero's canonical speech threshold; probabilities at or above it count as speech.
        const val SPEECH_THRESHOLD = 0.5f
        const val PCM_16BIT_MAX = 32_768f
    }
}
