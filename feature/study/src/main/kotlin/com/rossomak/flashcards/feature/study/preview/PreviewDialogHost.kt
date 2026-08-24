package com.rossomak.flashcards.feature.study.preview

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.feature.study.dialogs.SessionLengthDialog
import com.rossomak.flashcards.feature.study.dialogs.StudyModeDialog
import com.rossomak.flashcards.feature.study.dialogs.VoiceAnsweringDialog

/**
 * Renders whichever dialog [activeDialog] names, or nothing when it is `null`.
 *
 * The exhaustive `when` is the point: a new [PreviewDialog] case does not compile until it is
 * wired here, so a dialog can never be added to the state and silently never shown.
 */
@Composable
internal fun PreviewDialogHost(
    activeDialog: PreviewDialog?,
    availableTags: List<String>,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    when (activeDialog) {
        null -> Unit

        is PreviewDialog.Mode -> StudyModeDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(PreviewDialogEvent.DraftChange.Mode(it)) },
            onConfirm = { onDialogEvent(PreviewDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(PreviewDialogEvent.Dismiss) },
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(it)) },
        )

        is PreviewDialog.VoiceAnswering -> VoiceAnsweringDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(PreviewDialogEvent.DraftChange.VoiceAnswering(it)) },
            onConfirm = { onDialogEvent(PreviewDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(PreviewDialogEvent.Dismiss) },
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(it)) },
        )

        is PreviewDialog.Length -> SessionLengthDialog(
            draft = activeDialog.draft,
            range = StudySessionConfig.MIN_LENGTH..StudySessionConfig.MAX_LENGTH,
            step = StudySessionConfig.LENGTH_STEP,
            onDraftChange = { onDialogEvent(PreviewDialogEvent.DraftChange.Length(it)) },
            onConfirm = { onDialogEvent(PreviewDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(PreviewDialogEvent.Dismiss) },
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(it)) },
        )

        is PreviewDialog.Filters -> FlashcardFiltersDialog(
            availableTags = availableTags,
            filters = activeDialog.draft,
            difficultyBounds = StudySessionConfig.MIN_DIFFICULTY..StudySessionConfig.MAX_DIFFICULTY,
            onTagSelectedChange = { tag, isSelected ->
                onDialogEvent(PreviewDialogEvent.DraftChange.FilterTag(tag, isSelected))
            },
            onDifficultyRangeChange = { onDialogEvent(PreviewDialogEvent.DraftChange.FilterDifficulty(it)) },
            onConfirm = { onDialogEvent(PreviewDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(PreviewDialogEvent.Dismiss) },
        )

        is PreviewDialog.Sort -> FlashcardSortOrderDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(PreviewDialogEvent.DraftChange.SortOrder(it)) },
            onConfirm = { onDialogEvent(PreviewDialogEvent.Confirm) },
            onDismiss = { onDialogEvent(PreviewDialogEvent.Dismiss) },
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(PreviewDialogEvent.DraftChange.KeepAsDefault(it)) },
        )
    }
}
