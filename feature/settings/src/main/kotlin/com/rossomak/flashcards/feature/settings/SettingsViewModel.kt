package com.rossomak.flashcards.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.SetHasSeenOnboardingUseCase
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val setHasSeenOnboarding: SetHasSeenOnboardingUseCase,
    private val voiceSettingsController: VoiceSettingsController,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state: StateFlow<SettingsScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<SettingsDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        voiceSettingsController.bind(viewModelScope)
        viewModelScope.launch {
            voiceSettingsController.draftState.collect { draft ->
                _state.update { it.copy(voiceSettingsState = draft) }
            }
        }
    }

    fun onVoicePlaybackSettingsClick() {
        voiceSettingsController.open(viewModelScope)
    }

    fun onVoiceSettingsDraftVoiceChanged(voiceId: String?) {
        voiceSettingsController.onDraftVoiceChanged(voiceId)
    }

    fun onVoiceSettingsDraftSpeedChanged(speed: Float) {
        voiceSettingsController.onDraftSpeedChanged(speed)
    }

    fun onVoiceSettingsSave() {
        voiceSettingsController.save(viewModelScope)
    }

    fun onVoiceSettingsDismiss() {
        voiceSettingsController.dismiss()
    }

    /**
     * Debug affordance: clears the completion flag before navigating, so the flow behaves exactly
     * as it does for a first-run user — including committing preferences again on its final step —
     * rather than being a read-only walkthrough that behaves differently from the real thing.
     */
    fun onReplayOnboardingClick() {
        viewModelScope.launch {
            setHasSeenOnboarding(false)
            eventChannel.send(SettingsDestination.Onboarding)
        }
    }

    fun onSignOutClick() {
        if (_state.value.isSigningOut) {
            return
        }

        _state.update { it.copy(isSigningOut = true) }

        viewModelScope.launch {
            try {
                signOutUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Intentionally navigate to login even if remote sign-out fails.
            } finally {
                _state.update { it.copy(isSigningOut = false) }
                eventChannel.send(SettingsDestination.Login)
            }
        }
    }
}
