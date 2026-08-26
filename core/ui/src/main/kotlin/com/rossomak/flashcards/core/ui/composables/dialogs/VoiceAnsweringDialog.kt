package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.ui.R

/**
 * Turns hands-free voice answering on or off for Rated mode.
 *
 * A single-action dialog, not a yes/no fork: the user is choosing between two ongoing modes, not
 * confirming an irreversible act, so dismissing simply keeps whatever was already set.
 */
@Composable
fun VoiceAnsweringDialog(
    draft: Boolean,
    onDraftChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepAsDefault: Boolean? = null,
    onKeepAsDefaultChange: (Boolean) -> Unit = {},
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.voice_answering_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        keepAsDefault = keepAsDefault,
        onKeepAsDefaultChange = onKeepAsDefaultChange,
    ) {
        FlashcardsSingleSelectGroup {
            FlashcardsOptionCard(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.voice_answering_on_label),
                description = stringResource(R.string.voice_answering_on_message),
                selected = draft,
                onSelect = { onDraftChange(true) },
            )
            FlashcardsOptionCard(
                icon = Icons.Default.TouchApp,
                title = stringResource(R.string.voice_answering_off_label),
                description = stringResource(R.string.voice_answering_off_message),
                selected = !draft,
                onSelect = { onDraftChange(false) },
            )
        }
    }
}

@Preview
@Composable
private fun VoiceAnsweringDialogPreview() {
    VoiceAnsweringDialog(
        draft = true,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
        keepAsDefault = false,
    )
}
