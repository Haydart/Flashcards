package com.rossomak.flashcards.core.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.rossomak.flashcards.core.voice.SileroVadSession.Companion.SAMPLE_RATE_HZ
import java.nio.ByteBuffer
import java.nio.ByteOrder
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

    // Silero v5 prepends the previous chunk's last CONTEXT_SIZE samples to each new chunk, so the
    // model actually consumes CONTEXT_SIZE + 512 samples per step. Zeroed for the first chunk.
    private val context = FloatArray(CONTEXT_SIZE)

    override fun run(window: FloatArray): Float {
        val ortSession = session ?: environment.createSession(modelBytes).also { session = it }

        // Prepend the carried context so the model sees CONTEXT_SIZE + 512 samples, then remember
        // this chunk's tail as the next step's context. Without it the model scores everything ~0.
        val contextedWindow = FloatArray(CONTEXT_SIZE + window.size)
        context.copyInto(contextedWindow, destinationOffset = 0)
        window.copyInto(contextedWindow, destinationOffset = CONTEXT_SIZE)
        window.copyInto(context, destinationOffset = 0, startIndex = window.size - CONTEXT_SIZE)

        // Build from Java arrays / direct buffers only: ORT's native side reads a DIRECT buffer
        // here, and a heap FloatBuffer.wrap(...) silently delivers zeros instead of the audio.
        val inputTensor = OnnxTensor.createTensor(environment, arrayOf(contextedWindow)) // [1, 576]
        val stateTensor = OnnxTensor.createTensor(environment, stateAsBatchedArray()) // [2, 1, 128]
        val sampleRateTensor = OnnxTensor.createTensor(environment, directSampleRateBuffer(), longArrayOf())

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
        context.fill(0f)
    }

    /** Current recurrent state reshaped to the model's [2, 1, 128] input layout. */
    private fun stateAsBatchedArray(): Array<Array<FloatArray>> = Array(STATE_LAYERS) { layer ->
        arrayOf(FloatArray(STATE_HIDDEN) { unit -> state[layer * STATE_HIDDEN + unit] })
    }

    private fun directSampleRateBuffer(): LongBuffer = ByteBuffer.allocateDirect(java.lang.Long.BYTES)
        .order(ByteOrder.nativeOrder())
        .asLongBuffer()
        .apply {
            put(SAMPLE_RATE_HZ)
            rewind()
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
        const val CONTEXT_SIZE = 64 // Silero v5 context window for 16kHz
    }
}
