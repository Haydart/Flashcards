package com.rossomak.flashcards.core.voice

import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PitchShiftVoiceObfuscatorTest {

    private val obfuscator = PitchShiftVoiceObfuscator()

    private fun toneClip(durationSamples: Int = 16_000): ShortArray =
        ShortArray(durationSamples) { index ->
            (10_000 * sin(2 * PI * 220 * index / 16_000)).toInt().toShort()
        }

    @Test
    fun `obfuscate keeps duration within ten percent of the input`() {
        val input = toneClip()

        val output = obfuscator.obfuscate(input)

        val relativeLengthError = abs(output.size - input.size) / input.size.toDouble()
        relativeLengthError shouldBeLessThan 0.1
    }

    @Test
    fun `obfuscate changes the waveform`() {
        val input = toneClip()

        val output = obfuscator.obfuscate(input)

        val comparisonLength = minOf(input.size, output.size)
        var differingSamples = 0
        for (index in 0 until comparisonLength) {
            if (input[index] != output[index]) differingSamples++
        }
        (differingSamples > comparisonLength / 2) shouldBe true
    }

    @Test
    fun `obfuscate does not modify the input array`() {
        val input = toneClip()
        val inputCopy = input.copyOf()

        obfuscator.obfuscate(input)

        input shouldBe inputCopy
    }

    @Test
    fun `clip shorter than two windows is passed through as a copy`() {
        val shortClip = toneClip(durationSamples = 500)

        val output = obfuscator.obfuscate(shortClip)

        output shouldBe shortClip
        (output === shortClip) shouldBe false
    }

    @Test
    fun `randomizeSessionShift draws a new transform`() {
        val input = toneClip()
        val firstPass = obfuscator.obfuscate(input)

        // The shift is drawn from a continuous range, so two draws virtually never collide;
        // retry a few times to keep the test deterministic in practice.
        var changed = false
        repeat(5) {
            obfuscator.randomizeSessionShift()
            val nextPass = obfuscator.obfuscate(input)
            if (!nextPass.contentEquals(firstPass)) changed = true
        }
        changed shouldBe true
    }
}
