package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.ui.R

/**
 * Picks the order flashcards are presented in.
 *
 * Deferred commit, like every dialog: [draft] is the in-flight value and nothing is applied until
 * [onConfirm]. Dismissing (scrim tap, back press) discards the draft.
 *
 * Pass [keepAsDefault] as non-null where the choice is session-scoped and can optionally be
 * promoted to a permanent default (the Preview Study Session screen); pass `null` on the Settings
 * screen, where the change is permanent by definition.
 */
@Composable
fun FlashcardSortOrderDialog(
    draft: FlashcardSortOrder,
    onDraftChange: (FlashcardSortOrder) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepAsDefault: Boolean? = null,
    onKeepAsDefaultChange: (Boolean) -> Unit = {},
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.flashcard_sort_order_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        keepAsDefault = keepAsDefault,
        onKeepAsDefaultChange = onKeepAsDefaultChange,
    ) {
        FlashcardsSingleSelectGroup {
            FlashcardSortOrder.entries.forEach { sortOrder ->
                FlashcardsRadioRow(
                    label = sortOrder.label(),
                    selected = sortOrder == draft,
                    onSelect = { onDraftChange(sortOrder) },
                )
            }
        }
    }
}

@Composable
private fun FlashcardSortOrder.label(): String = when (this) {
    FlashcardSortOrder.Default -> stringResource(R.string.flashcard_sort_order_default_label)
    FlashcardSortOrder.EasiestFirst -> stringResource(R.string.flashcard_sort_order_easiest_first_label)
    FlashcardSortOrder.HardestFirst -> stringResource(R.string.flashcard_sort_order_hardest_first_label)
}

@Preview
@Composable
private fun FlashcardSortOrderDialogSessionPreview() {
    FlashcardSortOrderDialog(
        draft = FlashcardSortOrder.HardestFirst,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
        keepAsDefault = false,
    )
}

@Preview
@Composable
private fun FlashcardSortOrderDialogSettingsPreview() {
    FlashcardSortOrderDialog(
        draft = FlashcardSortOrder.Default,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
    )
}
