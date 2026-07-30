package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes

private const val STRIPE_CORNER_PERCENT = 50

/** Sample category color for previews — each real category defines its own, independently. */
private val sampleCategoryColor = Color(0xFF6B2FA0)

/**
 * A small category-coded vertical bar used as the leading slot of a search-result row, so a
 * topic result reads at a glance as belonging to its parent category. [color] is caller-supplied
 * — each flashcard category defines its own color independently, so `:core:ui` neither owns nor
 * derives it from `colorScheme` (categories aren't a design-system role, they're per-instance
 * domain data).
 */
@Composable
fun FlashcardsAccentStripe(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = MaterialTheme.sizes.accentStripeWidth, height = MaterialTheme.sizes.accentStripeHeight)
            .background(color = color, shape = RoundedCornerShape(percent = STRIPE_CORNER_PERCENT)),
    )
}

@ShowkaseComposable(name = "Accent stripe", group = "Lists")
@Composable
fun FlashcardsAccentStripeShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsAccentStripe(color = sampleCategoryColor)
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsAccentStripePreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsAccentStripe(color = sampleCategoryColor)
        }
    }
}
