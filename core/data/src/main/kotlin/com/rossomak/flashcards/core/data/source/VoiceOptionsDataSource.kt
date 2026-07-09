package com.rossomak.flashcards.core.data.source

import android.content.Context
import android.speech.tts.TextToSpeech
import com.rossomak.flashcards.core.data.voice.VoiceCuration
import com.rossomak.flashcards.core.domain.model.VoiceOption
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class VoiceOptionsDataSource @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun getAvailableVoices(): List<VoiceOption> = suspendCancellableCoroutine { continuation ->
        var tts: TextToSpeech? = null
        continuation.invokeOnCancellation { tts?.shutdown() }
        tts = TextToSpeech(context) { status ->
            val voices = if (status == TextToSpeech.SUCCESS) {
                VoiceCuration.curate(tts?.voices.orEmpty()).map { voice ->
                    val countryLabel = voice.locale.displayCountry.takeIf { it.isNotBlank() }
                        ?: voice.locale.displayLanguage
                    val shortName = voice.name.substringAfterLast(":")
                    VoiceOption(
                        id = voice.name,
                        displayName = "English ($countryLabel) · $shortName"
                    )
                }
            } else {
                emptyList()
            }
            tts?.shutdown()
            continuation.resume(voices)
        }
    }
}
