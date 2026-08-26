package com.rossomak.flashcards.core.ui.voice

import android.util.Log
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import com.rossomak.flashcards.core.domain.usecase.GetAvailableVoicesUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveVoiceSettingsUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveVoiceSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The voice-settings dialog's draft.
 *
 * Lives in the screen's `activeDialog` field like every other dialog's draft (ADR-0036), which is
 * why this controller neither holds nor exposes it — a second copy here would be a second source of
 * truth, and would let the draft outlive the dialog that owns it.
 */
data class VoiceSettingsDraftState(
    val availableVoices: List<VoiceOption> = emptyList(),
    val draftVoiceId: String? = null,
    val draftSpeed: Float = 1f,
)

/**
 * Owns the voice list cache, preview playback and saving for the voice-settings dialog. Shared by
 * feature:study and feature:settings; each ViewModel injects its own instance (unscoped) and binds
 * it to its own viewModelScope.
 */
class VoiceSettingsController @Inject constructor(
    private val observeVoiceSettings: ObserveVoiceSettingsUseCase,
    private val saveVoiceSettings: SaveVoiceSettingsUseCase,
    private val getAvailableVoices: GetAvailableVoicesUseCase,
    private val previewGateway: VoicePreviewGateway,
) {

    private var savedSettings = VoiceSettings()
    private var cachedVoices: List<VoiceOption>? = null

    val currentSettings: VoiceSettings get() = savedSettings

    fun bind(scope: CoroutineScope) {
        scope.launch {
            observeVoiceSettings().collect { settings -> savedSettings = settings }
        }
    }

    /**
     * The draft a newly opened dialog starts from — the saved settings, plus the voice list if it
     * is already cached. When it is not, [loadVoices] fills it in afterwards.
     */
    fun seedDraft(): VoiceSettingsDraftState {
        val voices = cachedVoices.orEmpty()
        return VoiceSettingsDraftState(
            availableVoices = voices,
            draftVoiceId = savedSettings.voiceId ?: voices.firstOrNull()?.id,
            draftSpeed = savedSettings.speechRate,
        )
    }

    /** No-op when the list is already cached, so an open never re-queries the platform. */
    fun loadVoices(scope: CoroutineScope, onLoaded: (List<VoiceOption>) -> Unit) {
        if (cachedVoices != null) return
        scope.launch {
            val voices = runCatching { getAvailableVoices() }.getOrDefault(emptyList())
            cachedVoices = voices
            onLoaded(voices)
        }
    }

    fun preview(draft: VoiceSettingsDraftState) {
        previewGateway.preview(draft.draftVoiceId, draft.draftSpeed)
    }

    fun save(scope: CoroutineScope, draft: VoiceSettingsDraftState): VoiceSettings {
        val settings = VoiceSettings(speechRate = draft.draftSpeed, voiceId = draft.draftVoiceId)
        scope.launch {
            runCatching { saveVoiceSettings(settings) }
                .onFailure { Log.e(TAG, "Failed to save voice settings", it) }
        }
        previewGateway.stop()
        return settings
    }

    fun stopPreview() {
        previewGateway.stop()
    }

    private companion object {
        const val TAG = "VoiceSettingsController"
    }
}
