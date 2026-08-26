package com.rossomak.flashcards.feature.settings

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceSettings

/**
 * Renders whichever dialog [activeDialog] names, or nothing when it is `null`.
 *
 * Takes nothing but the open dialog and the callback: everything a dialog needs to draw itself
 * travels inside its own case, so the host never grows a parameter per dialog (ADR-0036).
 */
@Composable
internal fun SettingsDialogHost(
    activeDialog: SettingsDialog?,
    onDialogEvent: (SettingsDialogEvent) -> Unit,
) {
    when (activeDialog) {
        null -> Unit

        is VoiceSettings -> VoiceSettingsDialog(
            availableVoices = activeDialog.draft.availableVoices,
            draftVoiceId = activeDialog.draft.draftVoiceId,
            draftSpeechRate = activeDialog.draft.draftSpeed,
            onDraftVoiceChange = {
                onDialogEvent(DraftChange(activeDialog.copy(draft = activeDialog.draft.copy(draftVoiceId = it))))
            },
            onDraftSpeechRateChange = {
                onDialogEvent(DraftChange(activeDialog.copy(draft = activeDialog.draft.copy(draftSpeed = it))))
            },
            onConfirm = { onDialogEvent(Confirm) },
            onDismiss = { onDialogEvent(Dismiss) },
        )
    }
}
