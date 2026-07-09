package com.rossomak.flashcards.core.voice

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wraps raw 16-bit PCM mono samples in a WAV (RIFF) container. Utterances are short
 * (seconds), so uncompressed WAV is used deliberately — see the design doc's Encoding section.
 */
object WavEncoder {

    private const val HEADER_SIZE_BYTES = 44
    private const val PCM_FORMAT_CODE: Short = 1
    private const val MONO_CHANNEL_COUNT: Short = 1
    private const val BITS_PER_SAMPLE: Short = 16

    fun encode(pcm: ShortArray, sampleRateHz: Int): ByteArray {
        val dataSizeBytes = pcm.size * Short.SIZE_BYTES
        val byteRate = sampleRateHz * MONO_CHANNEL_COUNT * BITS_PER_SAMPLE / 8
        val blockAlign = (MONO_CHANNEL_COUNT * BITS_PER_SAMPLE / 8).toShort()

        val header = ByteBuffer.allocate(HEADER_SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(HEADER_SIZE_BYTES - 8 + dataSizeBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(PCM_FORMAT_CODE)
            putShort(MONO_CHANNEL_COUNT)
            putInt(sampleRateHz)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(BITS_PER_SAMPLE)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSizeBytes)
        }

        val samples = ByteBuffer.allocate(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN)
        pcm.forEach { samples.putShort(it) }

        return ByteArrayOutputStream(HEADER_SIZE_BYTES + dataSizeBytes).apply {
            write(header.array())
            write(samples.array())
        }.toByteArray()
    }
}
