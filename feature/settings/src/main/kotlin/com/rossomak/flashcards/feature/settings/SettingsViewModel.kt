package com.rossomak.flashcards.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings as SavedVoiceSettings
import com.rossomak.flashcards.core.domain.usecase.SignOutUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.feature.settings.SettingsDialog.Attempts
import com.rossomak.flashcards.feature.settings.SettingsDialog.Length
import com.rossomak.flashcards.feature.settings.SettingsDialog.Mode
import com.rossomak.flashcards.feature.settings.SettingsDialog.ReadAloud
import com.rossomak.flashcards.feature.settings.SettingsDialog.SignOut
import com.rossomak.flashcards.feature.settings.SettingsDialog.Sort
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceAnswering
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
    private val voiceSettingsController: VoiceSettingsController,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state: StateFlow<SettingsScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<SettingsDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        // The controller owns the only subscription to the saved settings; this screen reads them
        // through it rather than collecting the same use case again, so there is one copy of the
        // value the voice row renders.
        voiceSettingsController.bind(viewModelScope, ::onVoiceSettingsChange)
        // The row shows the voice's name, not its id, so the list is needed before the dialog is
        // ever opened. Cached afterwards, so opening the dialog costs no second platform query.
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * Updates the row, and — since the dialog can open via [onVoiceSettingsOpen]'s
     * [VoiceSettingsController.seedDraft] before this settings snapshot ever arrives — fills an
     * already-open dialog's placeholder draft in with the real values too.
     */
    private fun onVoiceSettingsChange(settings: SavedVoiceSettings) {
        _state.update { state ->
            val withRow = state.copy(speechRate = settings.speechRate, voiceId = settings.voiceId)
            val dialog = withRow.activeDialog as? VoiceSettings ?: return@update withRow
            withRow.copy(activeDialog = dialog.copy(draft = voiceSettingsController.applySavedSettings(dialog.draft, settings)))
        }
    }

    /** Single entry point for every dialog on this screen. */
    fun onDialogEvent(event: SettingsDialogEvent) {
        when (event) {
            is Open -> onDialogOpen(event.dialog)
            is DraftChange -> onDraftChange(event.dialog)
            Confirm -> onDialogConfirm()
            Dismiss -> onDialogDismiss()
        }
    }

    /**
     * The caller hands over the dialog it wants shown, already seeded from the row it sits under.
     * Voice settings is the one exception — its draft comes from the controller, not from screen
     * state — so the ViewModel replaces the placeholder it is handed and kicks off the load.
     */
    private fun onDialogOpen(dialog: SettingsDialog) {
        when (dialog) {
            is VoiceSettings -> onVoiceSettingsOpen()
            else -> _state.update { it.copy(activeDialog = dialog) }
        }
    }

    private fun onVoiceSettingsOpen() {
        _state.update { it.copy(activeDialog = VoiceSettings(voiceSettingsController.seedDraft())) }
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * The voice list feeds two things: the row's summary, which needs it to turn the saved id into
     * a name, and an open voice dialog, which has to be found to be filled in — the one narrowing
     * cast left in the dialog path, once per load rather than once per edit. A dismissal in the
     * meantime correctly drops the dialog half.
     */
    private fun onVoicesLoaded(voices: List<VoiceOption>) {
        _state.update { state ->
            val withVoices = state.copy(availableVoices = voices)
            val dialog = withVoices.activeDialog as? VoiceSettings ?: return@update withVoices
            withVoices.copy(
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

    /**
     * Dismissal is the discard path: the draft dies with the field, so nothing is applied. Preview
     * playback is stopped only when it could have been started — every other dialog is silent, and
     * stopping the shared player from one of those could cut off audio this screen never began.
     */
    private fun onDialogDismiss() {
        if (_state.value.activeDialog is VoiceSettings) {
            voiceSettingsController.stopPreview()
        }
        _state.update { it.copy(activeDialog = null) }
    }

    /**
     * The only commit path. Every dialog folds its draft into screen state and closes; sign-out is
     * the one case with no draft, answering with an action instead (ADR-0036).
     *
     * TODO(settings-persistence): the study and voice-toggle values below live only in this
     *  ViewModel. Persist them through StudyPreferencesRepository once it exists — the same store
     *  the Preview screen's "Keep as my default" is waiting on (§10 of
     *  docs/temp/dialog-system-plan.md). Voice playback already persists, via the controller.
     */
    private fun onDialogConfirm() {
        val dialog = _state.value.activeDialog ?: return

        // The two cases whose commit is an action rather than a field. Kept out of the fold below
        // because `update` re-runs its lambda under contention, which would fire them twice.
        when (dialog) {
            is SignOut -> {
                _state.update { it.copy(activeDialog = null) }
                signOut()
                return
            }
            is VoiceSettings -> {
                voiceSettingsController.save(viewModelScope, dialog.draft)
                _state.update { it.copy(activeDialog = null) }
                return
            }
            else -> Unit
        }

        _state.update { state ->
            with(state) {
                when (dialog) {
                    is Length -> copy(sessionLength = dialog.draft)
                    is Attempts -> copy(ratedAttempts = dialog.draft)
                    is Mode -> copy(defaultStudyMode = dialog.draft)
                    is Sort -> copy(sortOrder = dialog.draft)
                    is VoiceAnswering -> copy(voiceAnsweringEnabled = dialog.draft)
                    is ReadAloud -> copy(readAloudEnabled = dialog.draft)
                    // Both returned above; repeated only because the `when` is exhaustive.
                    is VoiceSettings, SignOut -> this
                }
            }.copy(activeDialog = null)
        }
    }

    private fun signOut() {
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
