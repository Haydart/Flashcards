package com.rossomak.flashcards.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush

data class BrandColors(
    val topBarGradient: Brush,
    val ctaButtonGradient: Brush,
)

val lightBrandColors = BrandColors(
    topBarGradient = Brush.horizontalGradient(
        colors = listOf(primaryContainerLight, secondaryLight),
    ),
    ctaButtonGradient = Brush.horizontalGradient(
        colors = listOf(secondaryLight, primaryLight),
    ),
)

val darkBrandColors = BrandColors(
    topBarGradient = Brush.horizontalGradient(
        colors = listOf(primaryContainerDark, secondaryDark),
    ),
    ctaButtonGradient = Brush.horizontalGradient(
        colors = listOf(secondaryDark, primaryDark),
    ),
)

val LocalBrandColors = staticCompositionLocalOf { lightBrandColors }

val MaterialTheme.brandColors: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current
