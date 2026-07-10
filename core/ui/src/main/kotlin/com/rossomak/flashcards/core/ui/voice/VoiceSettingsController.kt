package com.rossomak.flashcards.core.ui.voice

import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import com.rossomak.flashcards.core.domain.usecase.GetAvailableVoicesUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveVoiceSettingsUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveVoiceSettingsUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceSettingsDraftState(
    val isVisible: Boolean = false,
    val availableVoices: List<VoiceOption> = emptyList(),
    val draftVoiceId: String? = null,
    val draftSpeed: Float = 1f,
)

/**
 * Owns the voice-settings dialog's draft state, voice list cache, and preview/save plumbing.
 * Shared by feature:study and feature:settings. Each ViewModel injects its own instance
 * (unscoped) and is responsible for binding it to its own viewModelScope.
 */
class VoiceSettingsController @Inject constructor(
    private val observeVoiceSettings: ObserveVoiceSettingsUseCase,
    private val saveVoiceSettings: SaveVoiceSettingsUseCase,
    private val getAvailableVoices: GetAvailableVoicesUseCase,
    private val previewGateway: VoicePreviewGateway,
) {

    private val _draftState = MutableStateFlow(VoiceSettingsDraftState())
    val draftState: StateFlow<VoiceSettingsDraftState> = _draftState.asStateFlow()

    private var savedSettings = VoiceSettings()
    private var cachedVoices: List<VoiceOption>? = null

    val currentSettings: VoiceSettings get() = savedSettings

    fun bind(scope: CoroutineScope) {
        scope.launch {
            observeVoiceSettings().collect { settings -> savedSettings = settings }
        }
    }

    fun open(scope: CoroutineScope) {
        _draftState.update {
            it.copy(
                isVisible = true,
                draftVoiceId = savedSettings.voiceId ?: cachedVoices?.firstOrNull()?.id,
                draftSpeed = savedSettings.speechRate,
            )
        }
        val cached = cachedVoices
        if (cached != null) {
            _draftState.update { it.copy(availableVoices = cached) }
        } else {
            scope.launch {
                val voices = runCatching { getAvailableVoices() }.getOrDefault(emptyList())
                cachedVoices = voices
                _draftState.update {
                    it.copy(
                        availableVoices = voices,
                        draftVoiceId = it.draftVoiceId ?: voices.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun onDraftVoiceChanged(voiceId: String?) {
        _draftState.update { it.copy(draftVoiceId = voiceId) }
        previewGateway.preview(voiceId, _draftState.value.draftSpeed)
    }

    fun onDraftSpeedChanged(speed: Float) {
        _draftState.update { it.copy(draftSpeed = speed) }
        previewGateway.preview(_draftState.value.draftVoiceId, speed)
    }

    fun save(scope: CoroutineScope): VoiceSettings {
        val settings = VoiceSettings(
            speechRate = _draftState.value.draftSpeed,
            voiceId = _draftState.value.draftVoiceId,
        )
        scope.launch { runCatching { saveVoiceSettings(settings) } }
        previewGateway.stop()
        _draftState.update { it.copy(isVisible = false) }
        return settings
    }

    fun dismiss() {
        previewGateway.stop()
        _draftState.update { it.copy(isVisible = false) }
    }
}
