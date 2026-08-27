package com.rossomak.flashcards.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardsDecisionDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.RatedAttemptsDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.ReadAloudDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.SessionLengthDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.StudyModeDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceAnsweringDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.settings.SettingsDialog.Attempts
import com.rossomak.flashcards.feature.settings.SettingsDialog.Goal
import com.rossomak.flashcards.feature.settings.SettingsDialog.Length
import com.rossomak.flashcards.feature.settings.SettingsDialog.Mode
import com.rossomak.flashcards.feature.settings.SettingsDialog.ReadAloud
import com.rossomak.flashcards.feature.settings.SettingsDialog.SignOut
import com.rossomak.flashcards.feature.settings.SettingsDialog.Sort
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceAnswering
import com.rossomak.flashcards.feature.settings.SettingsDialog.VoiceSettings

/**
 * Renders whichever dialog [activeDialog] names, or nothing when it is `null`.
 *
 * The exhaustive `when` is the point: a new [SettingsDialog] case does not compile until it is
 * wired here, so a dialog can never be added to the state and silently never shown.
 *
 * Takes nothing but the open dialog and the callback: everything a dialog needs to draw itself
 * travels inside its own case, so the host never grows a parameter per dialog (ADR-0036).
 *
 * Every dialog here passes `keepAsDefault = null` — a Settings value *is* the default, so there is
 * nothing to promote.
 */
@Composable
internal fun SettingsDialogHost(
    activeDialog: SettingsDialog?,
    onDialogEvent: (SettingsDialogEvent) -> Unit,
) {
    val onConfirm = { onDialogEvent(Confirm) }
    val onDismiss = { onDialogEvent(Dismiss) }

    when (activeDialog) {
        null -> Unit

        is Length -> SessionLengthDialog(
            draft = activeDialog.draft,
            range = StudySessionConfig.MIN_LENGTH..StudySessionConfig.MAX_LENGTH,
            step = StudySessionConfig.LENGTH_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is Goal -> DailyGoalDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is Attempts -> RatedAttemptsDialog(
            draft = activeDialog.draft,
            range = StudySessionConfig.MIN_RATED_ATTEMPTS..StudySessionConfig.MAX_RATED_ATTEMPTS,
            step = StudySessionConfig.RATED_ATTEMPTS_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is Mode -> StudyModeDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is Sort -> FlashcardSortOrderDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is VoiceAnswering -> VoiceAnsweringDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is ReadAloud -> ReadAloudDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

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
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        // Cancel and back both discard, so the decision dialog's single onCancel is Dismiss.
        SignOut -> FlashcardsDecisionDialog(
            title = stringResource(R.string.settings_sign_out_dialog_title),
            confirmLabel = stringResource(R.string.settings_sign_out_button),
            onConfirm = onConfirm,
            onCancel = onDismiss,
            icon = Icons.AutoMirrored.Filled.Logout,
            supportingText = stringResource(R.string.settings_sign_out_dialog_message),
        )
    }
}
