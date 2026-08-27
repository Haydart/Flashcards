package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rossomak.flashcards.core.domain.model.FlashcardSortOrder
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.ui.R
import java.util.Locale

/**
 * The committed-value labels the rows *above* these dialogs render — "Rated", "Easiest first",
 * "On".
 *
 * They live beside the dialogs rather than on each screen because a row and the dialog it opens
 * must name the same value identically: the Settings screen and the Preview Study Session screen
 * both show these values, and a second private copy per screen is how the two drift apart. They
 * are deliberately `@Composable` string lookups rather than fields on the enums — the domain layer
 * has no Android dependencies (AGENTS.md).
 */
@Composable
fun StudyMode.label(): String = when (this) {
    StudyMode.Rated -> stringResource(R.string.study_mode_rated_label)
    StudyMode.Fast -> stringResource(R.string.study_mode_fast_label)
}

@Composable
fun FlashcardSortOrder.label(): String = when (this) {
    FlashcardSortOrder.Default -> stringResource(R.string.flashcard_sort_order_default_label)
    FlashcardSortOrder.EasiestFirst -> stringResource(R.string.flashcard_sort_order_easiest_first_label)
    FlashcardSortOrder.HardestFirst -> stringResource(R.string.flashcard_sort_order_hardest_first_label)
}

/** Rated-mode voice *input*. Paired with [VoiceAnsweringDialog]. */
@Composable
fun voiceAnsweringLabel(isEnabled: Boolean): String = if (isEnabled) {
    stringResource(R.string.voice_answering_on_label)
} else {
    stringResource(R.string.voice_answering_off_label)
}

/** Fast-mode voice *output* plus hands-free advance. Paired with [ReadAloudDialog]. */
@Composable
fun readAloudLabel(isEnabled: Boolean): String = if (isEnabled) {
    stringResource(R.string.read_aloud_on_label)
} else {
    stringResource(R.string.read_aloud_off_label)
}

/**
 * Formats a speech rate as e.g. `1.25×`, capped at 2 decimal places. Fixed [Locale.US] so the
 * decimal separator is a dot regardless of device locale — the raw [Float] otherwise renders
 * with its full binary-to-decimal expansion (e.g. `1.0166667`).
 */
@Composable
fun speechRateLabel(speechRate: Float): String = stringResource(
    R.string.voice_settings_speed_value_label,
    String.format(Locale.US, "%.2f", speechRate).trimEnd('0').trimEnd('.'),
)
