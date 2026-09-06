package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.ui.R

/**
 * Turns re-queueing of Partial-rated Rated-mode cards on or off.
 *
 * `draft = true` is re-queueing on — the default — and asks the card again later in the session.
 * `draft = false` finishes the card on the spot instead, recording it Terminal Partial rather than
 * Mastered (ADR-0044), which the off option's message discloses rather than only promising the
 * card won't repeat.
 *
 * Same single-action shape as [ReadAloudDialog]/[VoiceAnsweringDialog]: the user is choosing
 * between two ongoing behaviours, not confirming an irreversible act, so dismissing simply keeps
 * whatever was already set.
 *
 * Pass [keepAsDefault] as non-null where the choice is session-scoped and can optionally be
 * promoted to a permanent default (the Preview Study Session screen); pass `null` on the Settings
 * screen, where the change is permanent by definition.
 */
@Composable
fun PartialRatingCardRequeueingDialog(
    draft: Boolean,
    onDraftChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    keepAsDefault: Boolean? = null,
    onKeepAsDefaultChange: (Boolean) -> Unit = {},
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.partial_rating_card_requeueing_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        keepAsDefault = keepAsDefault,
        onKeepAsDefaultChange = onKeepAsDefaultChange,
    ) {
        FlashcardsSingleSelectGroup {
            FlashcardsOptionCard(
                icon = Icons.Default.Replay,
                title = stringResource(R.string.partial_rating_card_requeueing_on_label),
                description = stringResource(R.string.partial_rating_card_requeueing_on_message),
                selected = draft,
                onSelect = { onDraftChange(true) },
            )
            FlashcardsOptionCard(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.partial_rating_card_requeueing_off_label),
                description = stringResource(R.string.partial_rating_card_requeueing_off_message),
                selected = !draft,
                onSelect = { onDraftChange(false) },
            )
        }
    }
}

@Preview
@Composable
private fun PartialRatingCardRequeueingDialogPreview() {
    PartialRatingCardRequeueingDialog(
        draft = true,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun PartialRatingCardRequeueingDialogSessionPreview() {
    PartialRatingCardRequeueingDialog(
        draft = true,
        onDraftChange = {},
        onConfirm = {},
        onDismiss = {},
        keepAsDefault = false,
    )
}
