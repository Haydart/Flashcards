package com.rossomak.flashcards.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.usecase.SetHasSeenOnboardingUseCase
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceSettings
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
    }

    /** Single entry point for every dialog on this screen. */
    fun onDialogEvent(event: SettingsDialogEvent) {
        when (event) {
            is Open -> onDialogOpen(event.dialog)
            is DraftChange -> onDraftChange(event.dialog)
            Confirm -> onDialogConfirm()
            Dismiss -> {
                voiceSettingsController.stopPreview()
                _state.update { it.copy(activeDialog = null) }
            }
        }
    }

    /**
     * The caller hands over the dialog it wants shown. Voice settings is the one this screen cannot
     * seed at the call site, so the ViewModel replaces the draft it is handed.
     */
    private fun onDialogOpen(dialog: SettingsDialog) {
        when (dialog) {
            is VoiceSettings -> onVoiceSettingsOpen()
        }
    }

    private fun onVoiceSettingsOpen() {
        _state.update { it.copy(activeDialog = VoiceSettings(voiceSettingsController.seedDraft())) }
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * The voice list arrives after the dialog is already up, so it has to find the open dialog to
     * fill in — the one narrowing cast left in the dialog path, once per open rather than once per
     * edit. A dismissal in the meantime correctly drops it.
     */
    private fun onVoicesLoaded(voices: List<VoiceOption>) {
        _state.update { state ->
            val dialog = state.activeDialog as? VoiceSettings ?: return@update state
            state.copy(
                activeDialog = dialog.copy(
                    draft = dialog.draft.copy(
                        availableVoices = voices,
                        draftVoiceId = dialog.draft.draftVoiceId ?: voices.firstOrNull()?.id,
                    ),
                ),
            )
        }
    }

    /**
     * Stores the draft the host built, then previews the edit. The trigger is a diff rather than a
     * typed per-field event, so every dialog stays on the one generic
     * [SettingsDialogEvent.DraftChange] and the preview stays unit-testable (ADR-0036).
     */
    private fun onDraftChange(dialog: SettingsDialog) {
        val previous = _state.value.activeDialog
        _state.update { it.copy(activeDialog = dialog) }
        if (previous is VoiceSettings &&
            dialog is VoiceSettings &&
            dialog.draft != previous.draft
        ) {
            voiceSettingsController.preview(dialog.draft)
        }
    }

    private fun onDialogConfirm() {
        val dialog = _state.value.activeDialog as? VoiceSettings ?: return
        voiceSettingsController.save(viewModelScope, dialog.draft)
        _state.update { it.copy(activeDialog = null) }
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
