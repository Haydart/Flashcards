package com.rossomak.flashcards.core.voice

import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * VTLP-style voice obfuscation: linear resampling shifts pitch *and* formants together by the
 * session's randomized factor, then a WSOLA time-stretch restores the original duration so the
 * transcript timing is unchanged. Keeps speech intelligible for STT while perturbing the raw
 * biometric voiceprint.
 *
 * Not cryptographically strong anonymization (see the design doc's stated caveat) — it deters
 * casual re-identification, not a determined speaker-identification model.
 */
class PitchShiftVoiceObfuscator @Inject constructor() : VoiceObfuscator {

    private var pitchFactor: Double = drawPitchFactor()

    override fun randomizeSessionShift() {
        pitchFactor = drawPitchFactor()
    }

    override fun obfuscate(pcm: ShortArray): ShortArray {
        if (pcm.size < WINDOW_SIZE_SAMPLES * 2) return pcm.copyOf()
        // Stretch first (duration * pitchFactor), then resample by pitchFactor: pitch and
        // formants shift by pitchFactor while overall duration returns to ~original.
        val stretched = timeStretch(pcm, pitchFactor)
        return resample(stretched, pitchFactor)
    }

    /** Random ±2–4 semitone shift; sign chosen per session so direction isn't predictable. */
    private fun drawPitchFactor(): Double {
        val semitones = MIN_SHIFT_SEMITONES + Random.nextDouble() * (MAX_SHIFT_SEMITONES - MIN_SHIFT_SEMITONES)
        val signedSemitones = if (Random.nextBoolean()) semitones else -semitones
        return 2.0.pow(signedSemitones / SEMITONES_PER_OCTAVE)
    }

    /**
     * WSOLA (waveform-similarity overlap-add) time stretch. Output duration ≈ input * [factor],
     * pitch unchanged. Overlapping analysis windows are re-laid at the synthesis hop, each
     * nudged within ±[MAX_SEARCH_OFFSET_SAMPLES] to the best waveform-similarity alignment.
     */
    private fun timeStretch(input: ShortArray, factor: Double): ShortArray {
        val synthesisHop = WINDOW_SIZE_SAMPLES / 2
        val analysisHop = (synthesisHop / factor).toInt().coerceAtLeast(1)
        val outputLength = (input.size * factor).toInt()
        val output = DoubleArray(outputLength + WINDOW_SIZE_SAMPLES)
        val windowWeight = DoubleArray(outputLength + WINDOW_SIZE_SAMPLES)
        val hannWindow = DoubleArray(WINDOW_SIZE_SAMPLES) { index ->
            0.5 * (1 - kotlin.math.cos(2.0 * Math.PI * index / (WINDOW_SIZE_SAMPLES - 1)))
        }

        var analysisPosition = 0
        var synthesisPosition = 0
        var previousWindowStart = 0
        while (analysisPosition + WINDOW_SIZE_SAMPLES + MAX_SEARCH_OFFSET_SAMPLES < input.size &&
            synthesisPosition + WINDOW_SIZE_SAMPLES < output.size
        ) {
            val windowStart = if (synthesisPosition == 0) {
                analysisPosition
            } else {
                bestAlignedStart(input, analysisPosition, previousWindowStart + synthesisHop)
            }
            for (index in 0 until WINDOW_SIZE_SAMPLES) {
                val weight = hannWindow[index]
                output[synthesisPosition + index] += input[windowStart + index] * weight
                windowWeight[synthesisPosition + index] += weight
            }
            previousWindowStart = windowStart
            analysisPosition += analysisHop
            synthesisPosition += synthesisHop
        }

        return ShortArray(outputLength) { index ->
            val weight = windowWeight[index]
            val sample = if (weight > MIN_OVERLAP_WEIGHT) output[index] / weight else output[index]
            sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /**
     * Searches ±[MAX_SEARCH_OFFSET_SAMPLES] around [candidateStart] for the window whose start
     * best continues the waveform laid down by the previous window (minimum absolute difference
     * against the natural continuation at [naturalContinuation]).
     */
    private fun bestAlignedStart(input: ShortArray, candidateStart: Int, naturalContinuation: Int): Int {
        var bestStart = candidateStart
        var bestScore = Double.MAX_VALUE
        val continuationStart = naturalContinuation.coerceIn(0, input.size - COMPARISON_LENGTH_SAMPLES - 1)
        for (offset in -MAX_SEARCH_OFFSET_SAMPLES..MAX_SEARCH_OFFSET_SAMPLES step SEARCH_STEP_SAMPLES) {
            val start = candidateStart + offset
            if (start < 0 || start + WINDOW_SIZE_SAMPLES >= input.size) continue
            var score = 0.0
            for (index in 0 until COMPARISON_LENGTH_SAMPLES) {
                score += abs((input[start + index] - input[continuationStart + index]).toDouble())
            }
            if (score < bestScore) {
                bestScore = score
                bestStart = start
            }
        }
        return bestStart
    }

    /** Linear-interpolation resampler; output length = input / [factor]. */
    private fun resample(input: ShortArray, factor: Double): ShortArray {
        val outputLength = (input.size / factor).toInt()
        return ShortArray(outputLength) { index ->
            val sourcePosition = index * factor
            val lowerIndex = sourcePosition.toInt().coerceAtMost(input.size - 1)
            val upperIndex = (lowerIndex + 1).coerceAtMost(input.size - 1)
            val fraction = sourcePosition - lowerIndex
            val sample = input[lowerIndex] * (1 - fraction) + input[upperIndex] * fraction
            sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private companion object {
        const val SEMITONES_PER_OCTAVE = 12.0
        const val MIN_SHIFT_SEMITONES = 2.0
        const val MAX_SHIFT_SEMITONES = 4.0
        const val WINDOW_SIZE_SAMPLES = 480 // 30ms at 16kHz
        const val MAX_SEARCH_OFFSET_SAMPLES = 120
        const val SEARCH_STEP_SAMPLES = 4
        const val COMPARISON_LENGTH_SAMPLES = 160
        const val MIN_OVERLAP_WEIGHT = 1e-3
    }
}
