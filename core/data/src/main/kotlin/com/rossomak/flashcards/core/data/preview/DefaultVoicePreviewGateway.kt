package com.rossomak.flashcards.core.data.preview

import android.content.Context
import android.speech.tts.TextToSpeech
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultVoicePreviewGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoicePreviewGateway {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingPreviewVoiceId: String? = null
    private var pendingPreviewRate: Float = 1f

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                applyPendingPreview()
            }
        }
    }

    override fun preview(voiceId: String?, speechRate: Float) {
        pendingPreviewVoiceId = voiceId
        pendingPreviewRate = speechRate
        ensureTts()
        if (ttsReady) applyPendingPreview()
    }

    private fun applyPendingPreview() {
        val engine = tts ?: return
        engine.stop()
        val targetVoiceId = pendingPreviewVoiceId
        if (targetVoiceId != null) {
            engine.voices?.firstOrNull { it.name == targetVoiceId }?.let { engine.voice = it }
        }
        engine.setSpeechRate(pendingPreviewRate)
        engine.speak(SAMPLE_TEXT, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_PREVIEW)
    }

    override fun stop() {
        tts?.stop()
    }

    companion object {
        private const val UTTERANCE_PREVIEW = "preview"
        private const val SAMPLE_TEXT =
            "Here is an example of how this voice sounds at the selected speed."
    }
}
