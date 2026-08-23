package com.rossomak.flashcards.feature.study.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.ui.composables.FlashcardsStepper
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardsSingleActionDialog
import com.rossomak.flashcards.feature.study.R

/**
 * Picks how many cards the session draws.
 *
 * Deferred commit — the stepper edits [draft] and nothing is applied until [onConfirm]. That is
 * what makes a stepper safe here: under live commit each tap would re-run card selection and, once
 * persistence lands, write a preference per tap.
 */
@Composable
fun SessionLengthDialog(
    draft: Int,
    range: IntRange,
    step: Int,
    onDraftChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepAsDefault: Boolean? = null,
    onKeepAsDefaultChange: (Boolean) -> Unit = {},
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.session_length_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        supportingText = stringResource(R.string.session_length_dialog_message),
        keepAsDefault = keepAsDefault,
        onKeepAsDefaultChange = onKeepAsDefaultChange,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pluralStringResource(R.plurals.session_length_cards_label, draft, draft),
                style = MaterialTheme.typography.titleMedium,
            )
            FlashcardsStepper(
                value = draft,
                onDecrement = { onDraftChange((draft - step).coerceAtLeast(range.first)) },
                onIncrement = { onDraftChange((draft + step).coerceAtMost(range.last)) },
                decrementContentDescription = stringResource(R.string.session_length_decrement_cd),
                incrementContentDescription = stringResource(R.string.session_length_increment_cd),
                decrementEnabled = draft > range.first,
                incrementEnabled = draft < range.last,
            )
        }
    }
}

@Preview
@Composable
private fun SessionLengthDialogPreview() {
    SessionLengthDialog(
        draft = 20,
        range = 10..50,
        step = 5,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
        keepAsDefault = false,
    )
}
