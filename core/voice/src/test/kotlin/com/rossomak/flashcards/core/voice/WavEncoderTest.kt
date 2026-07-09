package com.rossomak.flashcards.core.voice

import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Test

class WavEncoderTest {

    private val sampleRateHz = 16_000

    @Test
    fun `encode produces a RIFF WAVE header with correct chunk ids`() {
        val wav = WavEncoder.encode(ShortArray(160), sampleRateHz)

        String(wav, 0, 4, Charsets.US_ASCII) shouldBe "RIFF"
        String(wav, 8, 4, Charsets.US_ASCII) shouldBe "WAVE"
        String(wav, 12, 4, Charsets.US_ASCII) shouldBe "fmt "
        String(wav, 36, 4, Charsets.US_ASCII) shouldBe "data"
    }

    @Test
    fun `encode writes total size of header plus two bytes per sample`() {
        val sampleCount = 320
        val wav = WavEncoder.encode(ShortArray(sampleCount), sampleRateHz)

        wav.size shouldBe 44 + sampleCount * 2
    }

    @Test
    fun `encode writes sample rate and pcm samples little endian`() {
        val samples = shortArrayOf(1, -1, 12_345)
        val wav = WavEncoder.encode(samples, sampleRateHz)

        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        buffer.getInt(24) shouldBe sampleRateHz
        buffer.getInt(40) shouldBe samples.size * 2
        buffer.getShort(44) shouldBe 1.toShort()
        buffer.getShort(46) shouldBe (-1).toShort()
        buffer.getShort(48) shouldBe 12_345.toShort()
    }
}
