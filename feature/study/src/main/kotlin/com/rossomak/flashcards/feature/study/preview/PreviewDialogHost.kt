package com.rossomak.flashcards.feature.study.preview

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.withTag
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.study.dialogs.SessionLengthDialog
import com.rossomak.flashcards.feature.study.dialogs.StudyModeDialog
import com.rossomak.flashcards.feature.study.dialogs.VoiceAnsweringDialog
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering

/**
 * Renders whichever dialog [activeDialog] names, or nothing when it is `null`.
 *
 * The exhaustive `when` is the point: a new [PreviewDialog] case does not compile until it is
 * wired here, so a dialog can never be added to the state and silently never shown.
 *
 * Takes nothing but the open dialog and the callback: everything a dialog needs to draw itself
 * travels inside its own case, so the host never grows a parameter per dialog.
 *
 * Each branch emits a total `copy()` of the case the `when` already narrowed — no branching and no
 * arithmetic, because nothing unit-tests this file (ADR-0036).
 */
@Composable
internal fun PreviewDialogHost(
    activeDialog: PreviewDialog?,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    val onConfirm = { onDialogEvent(Confirm) }
    val onDismiss = { onDialogEvent(Dismiss) }

    when (activeDialog) {
        null -> Unit
        is Mode -> StudyModeDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = {
                onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it)))
            },
        )
        is VoiceAnswering -> VoiceAnsweringDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = {
                onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it)))
            },
        )
        is Length -> SessionLengthDialog(
            draft = activeDialog.draft,
            range = StudySessionConfig.MIN_LENGTH..StudySessionConfig.MAX_LENGTH,
            step = StudySessionConfig.LENGTH_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = {
                onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it)))
            },
        )
        is Filters -> FlashcardFiltersDialog(
            availableTags = activeDialog.availableTags,
            filters = activeDialog.draft,
            difficultyBounds = StudySessionConfig.MIN_DIFFICULTY..StudySessionConfig.MAX_DIFFICULTY,
            onTagSelectedChange = { tag, isSelected ->
                onDialogEvent(
                    DraftChange(activeDialog.copy(draft = activeDialog.draft.withTag(tag, isSelected)))
                )
            },
            onDifficultyRangeChange = {
                onDialogEvent(
                    DraftChange(activeDialog.copy(draft = activeDialog.draft.copy(difficultyRange = it)))
                )
            },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
        is Sort -> FlashcardSortOrderDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = {
                onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it)))
            },
        )
    }
}
