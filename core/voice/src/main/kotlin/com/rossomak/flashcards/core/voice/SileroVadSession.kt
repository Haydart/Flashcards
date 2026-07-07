package com.rossomak.flashcards.core.voice

/**
 * One inference seam over the Silero VAD ONNX model. Kept as an interface so the pure buffering /
 * thresholding logic in [SileroVoiceActivityDetector] is unit-testable on the JVM without the
 * Android-only `onnxruntime-android` native libraries (which only load on-device).
 *
 * The model is a stateful RNN: it must be fed contiguous, non-overlapping 512-sample windows in
 * order, and each call carries the recurrent state forward internally. [reset] clears that state
 * for a fresh capture session.
 */
interface SileroVadSession {

    /**
     * Runs one [WINDOW_SIZE_SAMPLES]-length window of 16kHz mono audio normalised to [-1, 1] and
     * returns the speech probability in [0, 1]. Advances the model's internal recurrent state.
     */
    fun run(window: FloatArray): Float

    /** Clears the recurrent state so the next [run] starts a new utterance context. */
    fun reset()

    companion object {
        /** Silero v5 requires exactly this window per inference at 16kHz. */
        const val WINDOW_SIZE_SAMPLES = 512
        const val SAMPLE_RATE_HZ = 16_000L
    }
}
