package com.rossomak.flashcards.core.data.source

import android.content.Context
import android.speech.tts.TextToSpeech
import com.rossomak.flashcards.core.data.voice.VoiceCuration
import com.rossomak.flashcards.core.domain.model.VoiceOption
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class VoiceOptionsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getAvailableVoices(): List<VoiceOption> = suspendCancellableCoroutine { continuation ->
        var tts: TextToSpeech? = null
        continuation.invokeOnCancellation { tts?.shutdown() }
        tts = TextToSpeech(context) { status ->
            val voices = if (status == TextToSpeech.SUCCESS) {
                // ISO country code (VoiceCuration guarantees one of US/GB/AU, never blank), not
                // Voice.locale.displayCountry: that's localized to the *device's* locale, so an
                // English voice's own country name would render in whatever language the user's
                // phone is set to (e.g. "Stany Zjednoczone" for "United States" on a Polish
                // device) — nonsensical paired with the fixed "English" label.
                VoiceCuration.curate(tts?.voices.orEmpty()).map { voice ->
                    val countryLabel = "English (${voice.locale.country})"
                    // VoiceCuration allows up to 3 voices per country, so countryLabel alone
                    // can't tell them apart — displayName appends the raw engine variant suffix
                    // (e.g. "x-tpf-local") so a picker listing every voice side by side stays
                    // disambiguated. shortLabel omits it: that suffix is internal engine detail,
                    // meaningless once a single voice is already chosen (the settings row summary
                    // only ever names the *current* selection, nothing to disambiguate against).
                    VoiceOption(
                        id = voice.name,
                        displayName = "$countryLabel · ${voice.name.substringAfterLast(":")}",
                        shortLabel = countryLabel,
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
