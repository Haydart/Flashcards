package com.rossomak.flashcards.feature.study.preview

import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.DefaultStudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.PartialRatingCardRequeueingEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.RatedAttempts
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.ReadAloudEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SessionLength
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SubcategoryCountRange as SubcategoryCountRangePreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoiceAnsweringEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoicePlayback
import com.rossomak.flashcards.core.ui.voice.toVoiceSettings
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

/**
 * Pure [StudySessionConfig]/[PreviewDialog] mapping helpers pulled out of
 * [PreviewStudySessionViewModel] — they touch no view-model state, so keeping them here instead
 * of as private class members keeps that class under detekt's `TooManyFunctions` threshold.
 */

/**
 * Lifted out of `onDialogConfirm` purely to keep that function under detekt's
 * `CyclomaticComplexMethod` threshold — every branch is still a total `copy()` of the config
 * with the confirmed dialog's draftState folded in.
 */
internal fun StudySessionConfig.foldInDialog(dialog: PreviewDialog): StudySessionConfig = when (dialog) {
    is Mode -> withMode(dialog.draftState)
    is VoiceAnswering -> copy(voiceAnsweringEnabled = dialog.draftState)
    is Attempts -> copy(ratedAttempts = dialog.draftState)
    is ReadAloud -> copy(readAloudEnabled = dialog.draftState)
    is PartialRatingCardRequeueing -> copy(partialRatingCardRequeueingEnabled = dialog.draftState)
    is Length -> copy(length = dialog.draftState)
    is Sort -> copy(sortOrder = dialog.draftState)
    is SubcategoryCountRange -> copy(subcategoryCountRange = dialog.draftState)
    is VoiceSettings -> copy(voiceSettings = dialog.draftState.toVoiceSettings())
    is Filters -> copy(
        tagIds = dialog.draftState.selectedTags,
        difficultyRange = dialog.draftState.difficultyRange,
    )
}

/**
 * `voiceAnsweringEnabled` is Rated-only (ADR-0025) — reset it switching away from Rated, not
 * just gate its *display* at the read sites, since the stale value would otherwise also leak
 * into `onStartSession`'s `RatedStudySessionRoute` payload unchanged. Its own function purely
 * to keep `onDialogConfirm`'s cyclomatic complexity under detekt's threshold.
 */
internal fun StudySessionConfig.withMode(mode: StudyMode): StudySessionConfig = copy(
    mode = mode,
    voiceAnsweringEnabled = voiceAnsweringEnabled && mode == StudyMode.Rated,
)

/**
 * `null` when the dialog didn't check "keep as my default" — or, for [Filters], can never
 * check it at all: tags belong to one subcategory and cannot carry to another (ADR-0030).
 */
internal fun PreviewDialog.toStudySessionPreferenceIfKept(): StudySessionPreference? = when (this) {
    is Mode -> DefaultStudyMode(draftState).takeIf { keepAsDefault }
    is VoiceAnswering -> VoiceAnsweringEnabled(draftState).takeIf { keepAsDefault }
    is Attempts -> RatedAttempts(draftState).takeIf { keepAsDefault }
    is ReadAloud -> ReadAloudEnabled(draftState).takeIf { keepAsDefault }
    is PartialRatingCardRequeueing -> PartialRatingCardRequeueingEnabled(draftState).takeIf { keepAsDefault }
    is Length -> SessionLength(draftState).takeIf { keepAsDefault }
    is Sort -> SortOrder(draftState).takeIf { keepAsDefault }
    is SubcategoryCountRange -> SubcategoryCountRangePreference(draftState).takeIf { keepAsDefault }
    is VoiceSettings -> VoicePlayback(draftState.toVoiceSettings()).takeIf { keepAsDefault }
    is Filters -> null
}
