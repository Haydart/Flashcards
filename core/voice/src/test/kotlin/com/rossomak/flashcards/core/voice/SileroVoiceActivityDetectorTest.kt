package com.rossomak.flashcards.core.voice

import com.rossomak.flashcards.core.voice.SileroVadSession.Companion.WINDOW_SIZE_SAMPLES
import io.kotest.matchers.shouldBe
import org.junit.Test

class SileroVoiceActivityDetectorTest {

    /** Scripted [SileroVadSession] returning queued probabilities and recording every window fed. */
    private class FakeSileroVadSession(probabilities: List<Float>) : SileroVadSession {
        private val queue = ArrayDeque(probabilities)
        val windowsRun = mutableListOf<FloatArray>()
        var resetCount = 0

        override fun run(window: FloatArray): Float {
            windowsRun += window.copyOf()
            return queue.removeFirstOrNull() ?: 0f
        }

        override fun reset() {
            resetCount++
        }
    }

    private val engineFrame = ShortArray(320) { Short.MAX_VALUE } // one 20ms capture frame

    @Test
    fun `a single sub-window frame runs no inference and reports silence`() {
        val session = FakeSileroVadSession(probabilities = emptyList())
        val detector = SileroVoiceActivityDetector(session)

        val isSpeech = detector.isSpeech(engineFrame)

        session.windowsRun.size shouldBe 0 // 320 < 512, nothing to classify yet
        isSpeech shouldBe false
    }

    @Test
    fun `a completed window is classified as speech when probability clears the threshold`() {
        val session = FakeSileroVadSession(probabilities = listOf(0.9f))
        val detector = SileroVoiceActivityDetector(session)

        detector.isSpeech(engineFrame) // 320 buffered, no window yet
        val isSpeech = detector.isSpeech(engineFrame) // 640 buffered -> one 512 window runs

        session.windowsRun.size shouldBe 1
        session.windowsRun.first().size shouldBe WINDOW_SIZE_SAMPLES
        isSpeech shouldBe true
    }

    @Test
    fun `a completed window below the threshold stays silence`() {
        val session = FakeSileroVadSession(probabilities = listOf(0.2f))
        val detector = SileroVoiceActivityDetector(session)

        detector.isSpeech(engineFrame)
        val isSpeech = detector.isSpeech(engineFrame)

        session.windowsRun.size shouldBe 1
        isSpeech shouldBe false
    }

    @Test
    fun `a partial frame after speech keeps the last speech classification instead of flipping to silence`() {
        val session = FakeSileroVadSession(probabilities = listOf(0.9f))
        val detector = SileroVoiceActivityDetector(session)

        detector.isSpeech(engineFrame)
        detector.isSpeech(engineFrame) shouldBe true // window classified as speech

        // A following 320-sample frame does not complete a new window (128 carry + 320 = 448).
        val stillSpeech = detector.isSpeech(engineFrame)

        session.windowsRun.size shouldBe 1 // no new inference
        stillSpeech shouldBe true // stays speech; a lone partial frame must not end the utterance
    }

    @Test
    fun `reset clears carried audio and resets the session`() {
        val session = FakeSileroVadSession(probabilities = listOf(0.9f, 0.9f))
        val detector = SileroVoiceActivityDetector(session)
        detector.isSpeech(engineFrame) // leaves 320 samples carried over

        detector.reset()

        session.resetCount shouldBe 1
        // With carry cleared, reaching a fresh window again takes two more frames, not one.
        detector.isSpeech(engineFrame)
        session.windowsRun.size shouldBe 0
        detector.isSpeech(engineFrame)
        session.windowsRun.size shouldBe 1
    }
}
