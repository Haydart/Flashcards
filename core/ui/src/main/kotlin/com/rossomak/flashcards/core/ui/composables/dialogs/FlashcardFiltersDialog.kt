package com.rossomak.flashcards.core.ui.composables.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rossomak.flashcards.core.ui.R
import com.rossomak.flashcards.core.ui.composables.FlashcardsDifficultyRangePill
import com.rossomak.flashcards.core.ui.composables.FlashcardsTagChip
import com.rossomak.flashcards.core.ui.theme.spacing
import kotlin.math.roundToInt

/**
 * Narrows a session's card pool by tag and difficulty.
 *
 * Deliberately has **no** "Keep as my default" checkbox: tags are per-subcategory, so a tag set
 * cannot meaningfully carry to a different subcategory, and filters stay session-scoped
 * (ADR-0030). That is expressed by simply not passing `keepAsDefault` down to the scaffold.
 *
 * Tags are OR-within (any selected tag matches) and AND-combined with the difficulty range.
 * [availableTags] is empty for multi-subcategory sessions, which filter by difficulty only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlashcardFiltersDialog(
    availableTags: List<String>,
    filters: FlashcardFilters,
    difficultyBounds: IntRange,
    onTagSelectedChange: (String, Boolean) -> Unit,
    onDifficultyRangeChange: (IntRange) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlashcardsSingleActionDialog(
        title = stringResource(R.string.flashcard_filters_dialog_title),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
            SectionLabel(text = stringResource(R.string.flashcard_filters_tags_label))
            if (availableTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.flashcard_filters_no_tags_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
                ) {
                    availableTags.forEach { tag ->
                        FlashcardsTagChip(
                            label = tag,
                            selected = tag in filters.selectedTags,
                            onSelectedChange = { selected -> onTagSelectedChange(tag, selected) },
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(
                    text = stringResource(R.string.flashcard_filters_difficulty_label),
                    modifier = Modifier.weight(1f),
                )
                FlashcardsDifficultyRangePill(
                    lowLevel = filters.difficultyRange.first,
                    highLevel = filters.difficultyRange.last,
                )
            }
            RangeSlider(
                value = filters.difficultyRange.first.toFloat()..filters.difficultyRange.last.toFloat(),
                onValueChange = { range ->
                    onDifficultyRangeChange(range.start.roundToInt()..range.endInclusive.roundToInt())
                },
                valueRange = difficultyBounds.first.toFloat()..difficultyBounds.last.toFloat(),
                // One step per whole difficulty level, minus the two endpoints.
                steps = (difficultyBounds.last - difficultyBounds.first - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview
@Composable
private fun FlashcardFiltersDialogPreview() {
    FlashcardFiltersDialog(
        availableTags = listOf("state", "recomposition", "side-effects", "modifiers"),
        filters = FlashcardFilters(selectedTags = setOf("state", "modifiers"), difficultyRange = 3..8),
        difficultyBounds = 1..10,
        onTagSelectedChange = { _, _ -> },
        onDifficultyRangeChange = {},
        onConfirm = {},
        onDismiss = {},
    )
}

@Preview
@Composable
private fun FlashcardFiltersDialogNoTagsPreview() {
    FlashcardFiltersDialog(
        availableTags = emptyList(),
        filters = FlashcardFilters(selectedTags = emptySet(), difficultyRange = 1..10),
        difficultyBounds = 1..10,
        onTagSelectedChange = { _, _ -> },
        onDifficultyRangeChange = {},
        onConfirm = {},
        onDismiss = {},
    )
}
