package com.rossomak.flashcards.feature.settings.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.CodeBlock
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardsDecisionDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.feature.settings.R
import com.rossomak.flashcards.feature.study.dialogs.ExtendedContextDialog
import com.rossomak.flashcards.feature.study.dialogs.ReportProblemDialog
import com.rossomak.flashcards.feature.study.dialogs.SessionLengthDialog
import com.rossomak.flashcards.feature.study.dialogs.StudyModeDialog
import com.rossomak.flashcards.feature.study.dialogs.VoiceAnsweringDialog

/**
 * Renders whichever dialog [activeDialog] names, and nothing when it is `null`.
 *
 * The `when` is exhaustive over [GalleryDialog], so adding a case without rendering it is a
 * compile error rather than a dialog that silently never opens. Keeping this in its own file is
 * the point of the pattern: the host absorbs the entire dialog surface of a screen, and the screen
 * itself carries two parameters instead of thirty.
 *
 * "Exit session?" is rendered straight from [FlashcardsDecisionDialog] with no concrete wrapper —
 * it is a title, a supporting line and two labels, with no mapping to own, so an
 * `ExitSessionDialog` file would be a pure rename.
 */
@Composable
internal fun DialogGalleryHost(
    activeDialog: GalleryDialog?,
    onDialogEvent: (GalleryDialogEvent) -> Unit,
) {
    val onDismiss = { onDialogEvent(GalleryDialogEvent.Dismiss) }
    val onConfirm = { onDialogEvent(GalleryDialogEvent.Confirm) }

    when (activeDialog) {
        null -> Unit

        is GalleryDialog.Sort -> FlashcardSortOrderDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(GalleryDialogEvent.SortDraftChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(GalleryDialogEvent.KeepAsDefaultChange(it)) },
        )

        is GalleryDialog.Mode -> StudyModeDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(GalleryDialogEvent.ModeDraftChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(GalleryDialogEvent.KeepAsDefaultChange(it)) },
        )

        is GalleryDialog.Length -> SessionLengthDialog(
            draft = activeDialog.draft,
            range = GalleryFixtures.SESSION_LENGTH_RANGE,
            step = GalleryFixtures.SESSION_LENGTH_STEP,
            onDraftChange = { onDialogEvent(GalleryDialogEvent.LengthDraftChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(GalleryDialogEvent.KeepAsDefaultChange(it)) },
        )

        is GalleryDialog.VoiceAnswering -> VoiceAnsweringDialog(
            draft = activeDialog.draft,
            onDraftChange = { onDialogEvent(GalleryDialogEvent.VoiceAnsweringDraftChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(GalleryDialogEvent.KeepAsDefaultChange(it)) },
        )

        is GalleryDialog.Voice -> VoiceSettingsDialog(
            availableVoices = activeDialog.availableVoices,
            draftVoiceId = activeDialog.draftVoiceId,
            draftSpeechRate = activeDialog.draftSpeechRate,
            onDraftVoiceChange = { onDialogEvent(GalleryDialogEvent.VoiceDraftVoiceChange(it)) },
            onDraftSpeechRateChange = { onDialogEvent(GalleryDialogEvent.VoiceDraftSpeechRateChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(GalleryDialogEvent.KeepAsDefaultChange(it)) },
        )

        is GalleryDialog.Filters -> FlashcardFiltersDialog(
            availableTags = GalleryFixtures.TAGS,
            filters = activeDialog.draft,
            difficultyBounds = GalleryFixtures.DIFFICULTY_BOUNDS,
            onTagSelectedChange = { tag, selected ->
                onDialogEvent(GalleryDialogEvent.TagSelectedChange(tag, selected))
            },
            onDifficultyRangeChange = { onDialogEvent(GalleryDialogEvent.DifficultyRangeChange(it)) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )

        is GalleryDialog.Report -> ReportProblemDialog(
            selectedActions = activeDialog.selectedActions,
            onActionCheckedChange = { action, checked ->
                onDialogEvent(GalleryDialogEvent.ReportActionCheckedChange(action, checked))
            },
            onSubmit = onConfirm,
            onCancel = onDismiss,
        )

        GalleryDialog.ExtendedContext -> ExtendedContextDialog(
            extendedContext = stringResource(R.string.dialog_gallery_extended_context_body),
            onDismiss = onDismiss,
            codeBlocks = listOf(CodeBlock(language = "kotlin", code = GalleryFixtures.CODE_SAMPLE)),
        )

        GalleryDialog.ExitSession -> FlashcardsDecisionDialog(
            title = stringResource(R.string.dialog_gallery_exit_title),
            confirmLabel = stringResource(R.string.dialog_gallery_exit_confirm_button),
            onConfirm = onConfirm,
            onCancel = onDismiss,
            icon = Icons.AutoMirrored.Filled.Logout,
            supportingText = stringResource(R.string.dialog_gallery_exit_message),
        )
    }
}
