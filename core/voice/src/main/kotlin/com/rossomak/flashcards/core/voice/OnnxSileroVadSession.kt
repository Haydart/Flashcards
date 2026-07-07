package com.rossomak.flashcards.core.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.rossomak.flashcards.core.voice.SileroVadSession.Companion.SAMPLE_RATE_HZ
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * [SileroVadSession] backed by ONNX Runtime. The [OrtSession] is created lazily on the first
 * [run] (model parse is a few ms and must not run on the DI/main thread), then reused for the
 * lifetime of the capture session. The [-1, 1] normalised audio, the carried recurrent [state],
 * and the fixed sample rate are the model's three inputs; it returns the speech probability plus
 * the next state, which is copied back for the following call.
 *
 * Not thread-safe: it is driven from the single [VoiceCaptureEngine] capture loop.
 */
class OnnxSileroVadSession(
    private val modelBytes: ByteArray,
) : SileroVadSession {

    private val environment: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var session: OrtSession? = null

    // Silero v5 recurrent state is shaped [2, 1, 128]; flattened here, zero-initialised.
    private val state = FloatArray(STATE_LAYERS * STATE_HIDDEN)

    override fun run(window: FloatArray): Float {
        val ortSession = session ?: environment.createSession(modelBytes).also { session = it }

        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(window),
            longArrayOf(1, window.size.toLong()),
        )
        val stateTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(state),
            longArrayOf(STATE_LAYERS.toLong(), 1, STATE_HIDDEN.toLong()),
        )
        val sampleRateTensor = OnnxTensor.createTensor(
            environment,
            LongBuffer.wrap(longArrayOf(SAMPLE_RATE_HZ)),
            longArrayOf(),
        )

        inputTensor.use { input ->
            stateTensor.use { previousState ->
                sampleRateTensor.use { sampleRate ->
                    val inputs = mapOf(
                        "input" to input,
                        "state" to previousState,
                        "sr" to sampleRate,
                    )
                    ortSession.run(inputs).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        val probability = (result[0].value as Array<FloatArray>)[0][0]
                        @Suppress("UNCHECKED_CAST")
                        val nextState = result[1].value as Array<Array<FloatArray>>
                        copyNextStateIn(nextState)
                        return probability
                    }
                }
            }
        }
    }

    override fun reset() {
        state.fill(0f)
    }

    private fun copyNextStateIn(nextState: Array<Array<FloatArray>>) {
        var index = 0
        for (layer in 0 until STATE_LAYERS) {
            val hidden = nextState[layer][0]
            for (unit in 0 until STATE_HIDDEN) {
                state[index++] = hidden[unit]
            }
        }
    }

    private companion object {
        const val STATE_LAYERS = 2
        const val STATE_HIDDEN = 128
    }
}
