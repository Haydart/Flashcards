package com.rossomak.flashcards.feature.study.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.R as CoreUiR
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheet
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheetHeader
import com.rossomak.flashcards.core.ui.composables.FlashcardsBottomSheetState
import com.rossomak.flashcards.core.ui.composables.FlashcardsDifficultyRangePill
import com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFilters
import com.rossomak.flashcards.core.ui.composables.dialogs.label
import com.rossomak.flashcards.core.ui.composables.dialogs.readAloudLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.speechRateLabel
import com.rossomak.flashcards.core.ui.composables.dialogs.voiceAnsweringLabel
import com.rossomak.flashcards.core.ui.composables.lists.FlashcardsSettingRow
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.theme.spacing
import com.rossomak.flashcards.feature.study.R
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.SubcategoryCountRange
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceSettings

/**
 * The settings sheet itself: [FlashcardsBottomSheet] docked over the ready screen, hidden until
 * the top bar's settings button opens it (ticket 07). [sheetState] and its open/closed value are
 * owned by the caller — this composable only renders what's inside once shown.
 *
 * [onContentTopChanged] reports this sheet's actual, live top edge in root coordinates on every
 * layout pass — including mid-drag and mid-animation — so a caller positioning content above the
 * sheet (ticket 08's HeroActions) can track it exactly. It has to be read from inside [content]
 * itself, not from [modifier]: [FlashcardsBottomSheet]'s underlying `BottomSheet` reports its own
 * outer bounds as its *unshifted* natural size and applies the open/closed offset only to the
 * child it places internally, so a `Modifier.onGloballyPositioned` on [modifier] would see a
 * fixed, bottom-anchored box regardless of hidden/expanded state — wrapping the header+rows here
 * instead observes the real, fully-resolved placement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionSettingsSheet(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    sheetState: FlashcardsBottomSheetState,
    onDismissRequest: () -> Unit,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
    onContentTopChanged: (Float) -> Unit = {},
) {
    FlashcardsBottomSheet(
        state = sheetState,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.onGloballyPositioned { onContentTopChanged(it.positionInRoot().y) }) {
            FlashcardsBottomSheetHeader(
                title = stringResource(R.string.preview_session_settings_sheet_title),
                onClose = onDismissRequest,
            )
            // weight(fill = false), not fillMaxHeight: a short row set should keep the sheet's
            // today's compact, content-hugging height, only capping — and scrolling — once the rows
            // actually run out of room (ticket 07).
            Column(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                SessionSettingRows(state = state, onDialogEvent = onDialogEvent)
            }
        }
    }
}

/**
 * One row per adjustable setting, each opening its own dialog (ADR-0030), through the shared
 * [FlashcardsSettingRow].
 *
 * Voice answering and attempts are Rated-only: Fast mode has no rating step for either to drive
 * (ADR-0025). Read-aloud is the Fast-only counterpart — voice *output* plus hands-free advance,
 * where voice answering is voice *input*. Each is not offered outside its mode rather than being
 * offered and ignored, so the two branches are exclusive. The Voice row itself only joins them
 * when something will actually speak: Fast with read-aloud on, or voice answering on — Fast alone
 * has nothing to configure yet, since read-aloud might still be off.
 */
@Composable
private fun SessionSettingRows(
    modifier: Modifier = Modifier,
    state: PreviewStudySessionScreenState,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_mode_label),
            valueText = state.config.mode.label(),
            onClick = { onDialogEvent(Open(Mode(draft = state.config.mode))) },
        )
        if (state.config.mode == StudyMode.Rated) {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_voice_answering_label),
                valueText = voiceAnsweringLabel(state.config.voiceAnsweringEnabled),
                onClick = {
                    onDialogEvent(
                        Open(VoiceAnswering(draft = state.config.voiceAnsweringEnabled))
                    )
                },
            )
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_attempts_label),
                valueText = pluralStringResource(
                    CoreUiR.plurals.rated_attempts_label,
                    state.config.ratedAttempts,
                    state.config.ratedAttempts,
                ),
                onClick = { onDialogEvent(Open(Attempts(draft = state.config.ratedAttempts))) },
            )
        } else {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_read_aloud_label),
                valueText = readAloudLabel(state.config.readAloudEnabled),
                onClick = { onDialogEvent(Open(ReadAloud(draft = state.config.readAloudEnabled))) },
            )
        }
        val fastModeSpeaksAloud = state.config.mode == StudyMode.Fast && state.config.readAloudEnabled
        if (fastModeSpeaksAloud || state.config.voiceAnsweringEnabled) {
            FlashcardsSettingRow(
                label = stringResource(R.string.preview_session_voice_settings_label),
                valueText = voicePlaybackSummary(state),
                onClick = { onDialogEvent(Open(VoiceSettings())) },
            )
        }
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_length_label),
            valueText = pluralStringResource(
                CoreUiR.plurals.session_length_cards_label,
                state.config.length,
                state.config.length,
            ),
            onClick = { onDialogEvent(Open(Length(draft = state.config.length))) },
        )
        if (state.isQuickSession) {
            SubcategoryCountRangeSettingRow(state = state, onDialogEvent = onDialogEvent)
        }
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_filters_label),
            value = { FiltersSettingValue(state = state) },
            onClick = {
                onDialogEvent(
                    Open(
                        Filters(
                            draft = FlashcardFilters(
                                selectedTags = state.config.tagIds,
                                difficultyRange = state.config.difficultyRange,
                            ),
                            availableTags = state.availableTags,
                        )
                    )
                )
            },
        )
        FlashcardsSettingRow(
            label = stringResource(R.string.preview_session_sort_label),
            valueText = state.config.sortOrder.label(),
            onClick = { onDialogEvent(Open(Sort(draft = state.config.sortOrder))) },
        )
    }
}

/**
 * Lifted out of [SessionSettingRows] purely to keep that function under detekt's `LongMethod` — a
 * single-Subcategory or Custom session has nothing to sample and never shows this row (ADR-0040).
 */
@Composable
private fun SubcategoryCountRangeSettingRow(
    state: PreviewStudySessionScreenState,
    onDialogEvent: (PreviewDialogEvent) -> Unit,
) {
    FlashcardsSettingRow(
        label = stringResource(R.string.preview_session_subcategory_count_range_label),
        valueText = stringResource(
            CoreUiR.string.subcategory_count_range_value_label,
            state.config.subcategoryCountRange.first,
            state.config.subcategoryCountRange.last,
        ),
        onClick = { onDialogEvent(Open(SubcategoryCountRange(draft = state.config.subcategoryCountRange))) },
    )
}

/**
 * Name and rate, or the rate alone while the platform voice list has not arrived yet or no longer
 * contains the saved id (an engine can be uninstalled between runs).
 */
@Composable
private fun voicePlaybackSummary(state: PreviewStudySessionScreenState): String {
    val rateLabel = speechRateLabel(state.config.voiceSettings.speechRate)
    val voiceName = state.availableVoices
        .firstOrNull { it.id == state.config.voiceSettings.voiceId }
        ?.shortLabel
        ?: return rateLabel
    return stringResource(R.string.preview_session_setting_value_separator_label, voiceName, rateLabel)
}

/**
 * The Filters row's value slot: a tag count beside [FlashcardsDifficultyRangePill], rather than
 * the plain text every other row uses (ticket 07). Difficulty always renders; the tag count only
 * joins it when tags are actually narrowing the pool.
 *
 * `config.tagIds` is materialized to every available tag by default (mirroring SubcategoryDetails,
 * ADR-0038), so "narrowing" means it is a *proper* subset of [PreviewStudySessionScreenState.availableTags]
 * — comparing against emptiness alone would show a tag count even when nothing was narrowed.
 */
@Composable
private fun FiltersSettingValue(state: PreviewStudySessionScreenState) {
    val config = state.config
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
    ) {
        if (config.tagIds.isNotEmpty() && config.tagIds != state.availableTags.toSet()) {
            Text(
                text = pluralStringResource(
                    R.plurals.preview_session_filters_tags_value_label,
                    config.tagIds.size,
                    config.tagIds.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlashcardsDifficultyRangePill(
            lowLevel = config.difficultyRange.first,
            highLevel = config.difficultyRange.last,
        )
    }
}
