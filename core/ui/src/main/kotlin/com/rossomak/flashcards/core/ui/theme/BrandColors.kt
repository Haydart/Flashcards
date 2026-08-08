package com.rossomak.flashcards.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class BrandColors(
    val topBarGradient: Brush,
    val onTopBarGradient: Color,
    val ctaButtonGradient: Brush,
)

/**
 * The brand gradient behind top bars and hero headers. Deliberately identical in both themes —
 * it is a brand asset, not a surface, so it does not follow light/dark. Only
 * [BrandColors.ctaButtonGradient] flips.
 */
private val brandTopBarGradient = Brush.horizontalGradient(
    colors = listOf(primaryContainerLight, secondaryLight),
)

/** Content colour for [brandTopBarGradient]; fixed, because the gradient itself never flips. */
private val onBrandTopBarGradient = Color.White

val lightBrandColors = BrandColors(
    topBarGradient = brandTopBarGradient,
    onTopBarGradient = onBrandTopBarGradient,
    ctaButtonGradient = Brush.horizontalGradient(
        colors = listOf(secondaryLight, primaryLight),
    ),
)

val darkBrandColors = BrandColors(
    topBarGradient = brandTopBarGradient,
    onTopBarGradient = onBrandTopBarGradient,
    ctaButtonGradient = Brush.horizontalGradient(
        colors = listOf(secondaryDark, primaryDark),
    ),
)

val LocalBrandColors = staticCompositionLocalOf { lightBrandColors }

val MaterialTheme.brandColors: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current
