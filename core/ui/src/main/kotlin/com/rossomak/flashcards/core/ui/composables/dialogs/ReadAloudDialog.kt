package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.ui.R

/**
 * Turns read-aloud auto-play on or off for Fast mode.
 *
 * The Fast-mode counterpart of [VoiceAnsweringDialog]: that one is Rated-only voice *input*, this
 * one is Fast-only voice *output* plus hands-free advance. Same single-action shape, because the
 * user is choosing between two ongoing modes rather than confirming an irreversible act —
 * dismissing simply keeps whatever was already set.
 *
 * Pass [keepAsDefault] as non-null where the choice is session-scoped and can optionally be
 * promoted to a permanent default (the Preview Study Session screen); pass `null` on the Settings
 * screen, where the change is permanent by definition.
 */
@Composable
fun ReadAloudDialog(
    draft: Boolean,
    onDraftChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepAsDefault: Boolean? = null,
    onKeepAsDefaultChange: (Boolean) -> Unit = {},
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.read_aloud_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        keepAsDefault = keepAsDefault,
        onKeepAsDefaultChange = onKeepAsDefaultChange,
    ) {
        FlashcardsSingleSelectGroup {
            FlashcardsOptionCard(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.read_aloud_on_label),
                description = stringResource(R.string.read_aloud_on_message),
                selected = draft,
                onSelect = { onDraftChange(true) },
            )
            FlashcardsOptionCard(
                icon = Icons.Default.TouchApp,
                title = stringResource(R.string.read_aloud_off_label),
                description = stringResource(R.string.read_aloud_off_message),
                selected = !draft,
                onSelect = { onDraftChange(false) },
            )
        }
    }
}

@Preview
@Composable
private fun ReadAloudDialogPreview() {
    ReadAloudDialog(
        draft = false,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun ReadAloudDialogSessionPreview() {
    ReadAloudDialog(
        draft = false,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
        keepAsDefault = false,
    )
}
