package com.rossomak.flashcards.feature.browse

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.withTag
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsDialog.Filters
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsDialog.Sort

/**
 * Renders whichever dialog this screen has open (ADR-0036).
 *
 * The `when` has already narrowed to a concrete case, so each branch emits a total `copy()` — no
 * per-field event type, and no cast. The host holds no state of its own: the draft lives in
 * [SubcategoryDetailsDialog] so dismissing discards it for free.
 */
@Composable
fun SubcategoryDetailsDialogHost(
    activeDialog: SubcategoryDetailsDialog?,
    onDialogEvent: (SubcategoryDetailsDialogEvent) -> Unit,
) {
    val onConfirm = { onDialogEvent(Confirm) }
    val onDismiss = { onDialogEvent(Dismiss) }

    when (activeDialog) {
        null -> Unit
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
        is Filters -> FlashcardFiltersDialog(
            availableTags = activeDialog.availableTags,
            filters = activeDialog.draft,
            difficultyBounds = activeDialog.difficultyBounds,
            onTagSelectedChange = { tag, isSelected ->
                onDialogEvent(DraftChange(activeDialog.copy(draft = activeDialog.draft.withTag(tag, isSelected))))
            },
            onDifficultyRangeChange = {
                onDialogEvent(DraftChange(activeDialog.copy(draft = activeDialog.draft.copy(difficultyRange = it))))
            },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
