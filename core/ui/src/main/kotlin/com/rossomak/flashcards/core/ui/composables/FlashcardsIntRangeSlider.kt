package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

/**
 * A [RangeSlider] over a low/high pair of whole numbers, snapped to one step per whole number —
 * the shared shape [FlashcardFiltersDialog][com.rossomak.flashcards.core.ui.composables.dialogs.FlashcardFiltersDialog]'s
 * Difficulty facet and
 * [SubcategoryCountRangeDialog][com.rossomak.flashcards.core.ui.composables.dialogs.SubcategoryCountRangeDialog]
 * both need, pulled out once a second caller showed up rather than copied a third time.
 */
@Composable
fun FlashcardsIntRangeSlider(
    value: IntRange,
    bounds: IntRange,
    onValueChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    RangeSlider(
        value = value.first.toFloat()..value.last.toFloat(),
        onValueChange = { range ->
            onValueChange(range.start.roundToInt()..range.endInclusive.roundToInt())
        },
        valueRange = bounds.first.toFloat()..bounds.last.toFloat(),
        // One step per whole number, minus the two endpoints.
        steps = (bounds.last - bounds.first - 1).coerceAtLeast(0),
        modifier = modifier.fillMaxWidth(),
    )
}
