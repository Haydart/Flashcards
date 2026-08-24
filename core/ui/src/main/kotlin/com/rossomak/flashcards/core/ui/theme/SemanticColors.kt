package com.rossomak.flashcards.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Valence colors — the green / amber / red container+content pairs shared by every component that
 * encodes "good, middling, bad" on a themed surface: the study screen's rating buttons and
 * [com.rossomak.flashcards.core.ui.composables.banners.FlashcardsInfoBanner].
 *
 * Named by valence rather than by either consumer's vocabulary, so both read naturally at the call
 * site — a card rated "Not at all" and a banner flagging a restriction both resolve to
 * [negativeContainer], and neither has to borrow the other's domain language.
 *
 * Unlike [BrandColors]' on-gradient colors these **do** flip light/dark: they sit on a themed
 * surface, not on the fixed brand gradient.
 *
 * Each pair is authored explicitly rather than derived as one accent at an alpha, for two reasons.
 * The light values are the rating-button palette that already shipped, and its containers are more
 * saturated than any uniform tint of their content color (`onNegativeContainerLight` over white at
 * 12% gives `#F8EAEA`, not `#F6D9DA`). And an alpha-composited container would change with
 * whatever sits behind it, so the same banner would read differently on `surface` than inside a
 * card.
 */
data class SemanticColors(
    val positiveContainer: Color,
    val onPositiveContainer: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val negativeContainer: Color,
    val onNegativeContainer: Color,
    val onGradientLossContainer: Color,
)

/**
 * The container fill/border base for a `Loss` row on the brand gradient — currently
 * [com.rossomak.flashcards.core.ui.composables.banners.FlashcardsXpBreakdownRow]. Reuses
 * [errorContainerDark] (the same saturated maroon in both themes): unlike every other pair in this
 * file, it does **not** flip light/dark, because the gradient it sits on doesn't either — it lives
 * here rather than in [BrandColors] because it is still a valence color, grouped with this file's
 * other container/on-container pairs rather than the fixed brand-asset tokens.
 *
 * Deliberately not [negativeContainer]: that pair is tuned for a themed surface and reads too
 * washed-out once tinted onto the gradient at a usable alpha.
 */
private val onGradientLossContainerColor = errorContainerDark

val lightSemanticColors = SemanticColors(
    positiveContainer = positiveContainerLight,
    onPositiveContainer = onPositiveContainerLight,
    neutralContainer = neutralContainerLight,
    onNeutralContainer = onNeutralContainerLight,
    negativeContainer = negativeContainerLight,
    onNegativeContainer = onNegativeContainerLight,
    onGradientLossContainer = onGradientLossContainerColor,
)

val darkSemanticColors = SemanticColors(
    positiveContainer = positiveContainerDark,
    onPositiveContainer = onPositiveContainerDark,
    neutralContainer = neutralContainerDark,
    onNeutralContainer = onNeutralContainerDark,
    negativeContainer = negativeContainerDark,
    onNegativeContainer = onNegativeContainerDark,
    onGradientLossContainer = onGradientLossContainerColor,
)

val LocalSemanticColors = staticCompositionLocalOf { lightSemanticColors }

val MaterialTheme.semanticColors: SemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSemanticColors.current
