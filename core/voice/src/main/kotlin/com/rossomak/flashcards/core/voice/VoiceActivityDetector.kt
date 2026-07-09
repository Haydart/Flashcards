package com.rossomak.flashcards.core.voice

/**
 * Per-frame speech/silence classifier. Implementations must be cheap enough to run on every
 * 20ms PCM frame on-device. Utterance segmentation (start/end boundaries, hangover) is layered
 * on top by [VoiceCaptureEngine].
 */
interface VoiceActivityDetector {

    /** Returns true when [frame] (16kHz mono PCM) is classified as speech. */
    fun isSpeech(frame: ShortArray): Boolean

    /** Clears adaptive state so a new capture session starts from a clean noise estimate. */
    fun reset()
}
