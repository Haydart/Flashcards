package com.rossomak.flashcards.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.ui.theme.ratingColors
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

// TODO(core-ui): replace with the shared attempts indicator once it lands in :core:ui. The real
//  component is owned by the study-session card, which needs states this stand-in does not model
//  (attempt in progress, attempts exhausted); onboarding only ever draws a fixed illustration.

private val AttemptDotSize = 10.dp

/**
 * The per-Attempt outcome dots on a Rated flashcard: one dot per configured Attempt, filled with
 * that attempt's Rating colour or left as an empty outline while unused.
 *
 * The row is hidden from accessibility — it is decoration inside an illustration whose surrounding
 * copy already states the mechanic, so announcing it would only add noise.
 */
@Composable
fun OnboardingAttemptsIndicator(
    attemptRatings: List<FlashcardRating?>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        attemptRatings.forEach { rating ->
            val dotModifier = Modifier.size(AttemptDotSize)
            if (rating == null) {
                Box(
                    modifier = dotModifier.border(
                        width = MaterialTheme.sizes.hairline,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
                )
            } else {
                Box(
                    modifier = dotModifier.background(
                        color = MaterialTheme.ratingColors.contentColorFor(rating),
                        shape = CircleShape,
                    ),
                )
            }
        }
    }
}
