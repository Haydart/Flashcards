package com.rossomak.flashcards.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    /** Single entry point for every dialog on this screen. */
    fun onDialogEvent(event: SettingsDialogEvent) {
        when (event) {
            SettingsDialogEvent.Open.VoiceSettings -> {
                voiceSettingsController.open(viewModelScope)
                _state.update { it.copy(activeDialog = SettingsDialog.VoiceSettings) }
            }
            is SettingsDialogEvent.DraftChange.VoiceSettingsVoice ->
                voiceSettingsController.onDraftVoiceChanged(event.voiceId)
            is SettingsDialogEvent.DraftChange.VoiceSettingsSpeechRate ->
                voiceSettingsController.onDraftSpeedChanged(event.speechRate)
            SettingsDialogEvent.Confirm -> {
                voiceSettingsController.save(viewModelScope)
                _state.update { it.copy(activeDialog = null) }
            }
            SettingsDialogEvent.Dismiss -> {
                voiceSettingsController.dismiss()
                _state.update { it.copy(activeDialog = null) }
            }
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
