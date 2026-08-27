package com.rossomak.flashcards.feature.study.preview

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.RatedAttemptsDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.ReadAloudDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.SessionLengthDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.StudyModeDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceAnsweringDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.withTag
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
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
        is Attempts -> RatedAttemptsDialog(
            draft = activeDialog.draft,
            range = StudySessionConfig.MIN_RATED_ATTEMPTS..StudySessionConfig.MAX_RATED_ATTEMPTS,
            step = StudySessionConfig.RATED_ATTEMPTS_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draft = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = {
                onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it)))
            },
        )
        is ReadAloud -> ReadAloudDialog(
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
        is Filters -> FiltersDialog(
            dialog = activeDialog,
            onDialogEvent = onDialogEvent,
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

/**
 * The one case whose edits are not a single field assignment — a tag toggle folds through
 * [withTag] — so it is lifted out of [PreviewDialogHost]'s `when` to keep that dispatch readable.
 * Still only a total `copy()` of an already-narrowed case, as ADR-0036 requires of a host.
 */
@Composable
private fun FiltersDialog(
    dialog: Filters,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FlashcardFiltersDialog(
        availableTags = dialog.availableTags,
        filters = dialog.draft,
        difficultyBounds = StudySessionConfig.MIN_DIFFICULTY..StudySessionConfig.MAX_DIFFICULTY,
        onTagSelectedChange = { tag, isSelected ->
            onDialogEvent(DraftChange(dialog.copy(draft = dialog.draft.withTag(tag, isSelected))))
        },
        onDifficultyRangeChange = {
            onDialogEvent(DraftChange(dialog.copy(draft = dialog.draft.copy(difficultyRange = it))))
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
