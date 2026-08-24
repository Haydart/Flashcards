package com.rossomak.flashcards.feature.study.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.ui.R as CoreUiR
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardsDecisionDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsDraftState
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.dialogs.ExtendedContextDialog
import com.rossomak.flashcards.feature.study.dialogs.ReportProblemDialog

/**
 * Renders whichever dialog [activeDialog] names, or nothing when it is `null`.
 *
 * "Exit session?" has no named wrapper by design — it is a title, a supporting line and two
 * labels, so wrapping [FlashcardsDecisionDialog] would be a pure rename. Its confirm is the only
 * one that does not travel through [onDialogEvent]: exiting is navigation, and there is no draft
 * to commit.
 */
@Composable
internal fun StudySessionDialogHost(
    activeDialog: StudySessionDialog?,
    currentCard: Flashcard?,
    voiceSettingsState: VoiceSettingsDraftState,
    onDialogEvent: (StudySessionDialogEvent) -> Unit,
    onExitSession: () -> Unit,
) {
    when (activeDialog) {
        null -> Unit

        is StudySessionDialog.ReportProblem -> ReportProblemDialog(
            selectedActions = activeDialog.selectedActions,
            onActionCheckedChange = { action, isChecked ->
                onDialogEvent(StudySessionDialogEvent.DraftChange.ReportProblemAction(action, isChecked))
            },
            onSubmit = { onDialogEvent(StudySessionDialogEvent.Confirm) },
            onCancel = { onDialogEvent(StudySessionDialogEvent.Dismiss) },
        )

        StudySessionDialog.ExtendedContext -> {
            val extendedContext = currentCard?.extendedContext
            if (!extendedContext.isNullOrBlank()) {
                ExtendedContextDialog(
                    extendedContext = extendedContext,
                    onDismiss = { onDialogEvent(StudySessionDialogEvent.Dismiss) },
                )
            }
        }

        StudySessionDialog.VoiceAnswerConsent -> FlashcardsDecisionDialog(
            title = stringResource(R.string.study_session_voice_answer_consent_title),
            confirmLabel = stringResource(R.string.study_session_voice_answer_consent_accept_button),
            onConfirm = { onDialogEvent(StudySessionDialogEvent.Confirm) },
            onCancel = { onDialogEvent(StudySessionDialogEvent.Dismiss) },
            supportingText = stringResource(R.string.study_session_voice_answer_consent_message),
            cancelLabel = stringResource(R.string.study_session_voice_answer_consent_decline_button),
        )

        StudySessionDialog.VoiceSettings -> VoiceSettingsDialog(
            availableVoices = voiceSettingsState.availableVoices,
            draftVoiceId = voiceSettingsState.draftVoiceId,
            draftSpeechRate = voiceSettingsState.draftSpeed,
            onDraftVoiceChange = { onDialogEvent(StudySessionDialogEvent.DraftChange.VoiceSettingsVoice(it)) },
            onDraftSpeechRateChange = { onDialogEvent(StudySessionDialogEvent.DraftChange.VoiceSettingsSpeechRate(it)) },
            onConfirm = { onDialogEvent(StudySessionDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(StudySessionDialogEvent.Dismiss) },
        )

        StudySessionDialog.ExitSession -> FlashcardsDecisionDialog(
            title = stringResource(R.string.exit_session_dialog_title),
            confirmLabel = stringResource(R.string.exit_session_confirm_button),
            onConfirm = {
                onDialogEvent(StudySessionDialogEvent.Dismiss)
                onExitSession()
            },
            onCancel = { onDialogEvent(StudySessionDialogEvent.Dismiss) },
            supportingText = stringResource(R.string.exit_session_dialog_message),
            cancelLabel = stringResource(CoreUiR.string.common_cancel_button),
        )
    }
}
