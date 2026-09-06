package com.rossomak.flashcards.feature.study.preview

import androidx.compose.runtime.Composable
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionConfig.Companion.LENGTH_STEP
import com.rossomak.flashcards.core.domain.model.StudySessionConfig.Companion.RATED_ATTEMPTS_STEP
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardSortOrderDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.PartialRatingCardRequeueingDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.RatedAttemptsDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.ReadAloudDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.SessionLengthDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.StudyModeDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.SubcategoryCountRangeDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceAnsweringDialog
import com.rossomak.flashcards.core.ui.composables.dialogs.VoiceSettingsDialog
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.PartialRatingCardRequeueing
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.SubcategoryCountRange
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceSettings

/** The one range each stepper dialog needs — named once so a call site never inlines the pair. */
private val LENGTH_RANGE = StudySessionConfig.MIN_LENGTH..StudySessionConfig.MAX_LENGTH
private val RATED_ATTEMPTS_RANGE = StudySessionConfig.MIN_RATED_ATTEMPTS..StudySessionConfig.MAX_RATED_ATTEMPTS
private val SUBCATEGORY_COUNT_RANGE =
    StudySessionConfig.MIN_SUBCATEGORY_COUNT..StudySessionConfig.MAX_SUBCATEGORY_COUNT
private val DIFFICULTY_RANGE = StudySessionConfig.MIN_DIFFICULTY..StudySessionConfig.MAX_DIFFICULTY

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
@Suppress("LongMethod")
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
            draft = activeDialog.draftState,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is VoiceAnswering -> VoiceAnsweringDialog(
            draft = activeDialog.draftState,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is Attempts -> RatedAttemptsDialog(
            draft = activeDialog.draftState,
            range = RATED_ATTEMPTS_RANGE,
            step = RATED_ATTEMPTS_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is PartialRatingCardRequeueing -> PartialRatingCardRequeueingDialog(
            draft = activeDialog.draftState,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is ReadAloud -> ReadAloudDialog(
            draft = activeDialog.draftState,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is Length -> SessionLengthDialog(
            draft = activeDialog.draftState,
            range = LENGTH_RANGE,
            step = LENGTH_STEP,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is Sort -> FlashcardSortOrderDialog(
            draft = activeDialog.draftState,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is SubcategoryCountRange -> SubcategoryCountRangeDialog(
            draft = activeDialog.draftState,
            bounds = SUBCATEGORY_COUNT_RANGE,
            onDraftChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is VoiceSettings -> VoiceSettingsDialog(
            availableVoices = activeDialog.draftState.availableVoices,
            draftVoiceId = activeDialog.draftState.draftVoiceId,
            onDraftVoiceChange = {
                onDialogEvent(DraftChange(activeDialog.copy(draftState = activeDialog.draftState.copy(draftVoiceId = it))))
            },
            draftSpeechRate = activeDialog.draftState.draftSpeed,
            onDraftSpeechRateChange = {
                onDialogEvent(DraftChange(activeDialog.copy(draftState = activeDialog.draftState.copy(draftSpeed = it))))
            },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            keepAsDefault = activeDialog.keepAsDefault,
            onKeepAsDefaultChange = { onDialogEvent(DraftChange(activeDialog.copy(keepAsDefault = it))) },
        )
        is Filters -> FlashcardFiltersDialog(
            availableTags = activeDialog.availableTags,
            filters = activeDialog.draftState,
            difficultyBounds = DIFFICULTY_RANGE,
            onFiltersChange = { onDialogEvent(DraftChange(activeDialog.copy(draftState = it))) },
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}
