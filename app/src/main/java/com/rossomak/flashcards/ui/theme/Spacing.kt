package com.rossomak.flashcards.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppSpacing(
    val none: Dp = 0.dp,
    val xxsmall: Dp = 4.dp,
    val xsmall: Dp = 8.dp,
    val small: Dp = 12.dp,
    val normal: Dp = 16.dp,
    val medium: Dp = 24.dp,
    val large: Dp = 32.dp,
    val xlarge: Dp = 48.dp,
    val xxlarge: Dp = 64.dp,
)

val LocalSpacing = staticCompositionLocalOf { AppSpacing() }

val MaterialTheme.spacing: AppSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
