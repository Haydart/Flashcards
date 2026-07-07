package com.rossomak.flashcards.core.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal on-device playback of raw PCM clips. Used by the debug Voice screen to audition
 * captured audio and the obfuscation A/B — not part of the production study-session flow
 * (card playback stays on the Media3 [TtsPlayer] stack).
 */
@Singleton
class PcmPlayer @Inject constructor() {

    private var audioTrack: AudioTrack? = null

    fun play(pcm: ShortArray, sampleRateHz: Int = VoiceCaptureEngine.SAMPLE_RATE_HZ) {
        stop()
        if (pcm.isEmpty()) return
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        audioTrack = track
    }

    fun stop() {
        audioTrack?.let { track ->
            runCatching { track.stop() }
            track.release()
        }
        audioTrack = null
    }
}
