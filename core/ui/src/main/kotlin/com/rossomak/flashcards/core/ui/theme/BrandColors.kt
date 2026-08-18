package com.rossomak.flashcards.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class BrandColors(
    val topBarGradient: Brush,
    val onTopBarGradient: Color,
    val ctaButtonGradient: Brush,
    val onGradientFilled: Color,
    val tonalButtonContainer: Color,
    val onTonalButtonContainer: Color,
)

/**
 * The brand gradient behind top bars and hero headers. Deliberately identical in both themes —
 * it is a brand asset, not a surface, so it does not follow light/dark.
 */
private val brandTopBarGradient = Brush.horizontalGradient(
    colors = listOf(primaryContainerLight, secondaryLight),
)

/** Content colour for [brandTopBarGradient]; fixed, because the gradient itself never flips. */
private val onBrandTopBarGradient = Color.White

/** Indigo stop of [brandCtaButtonGradient] — the brand's CTA gradient, fixed across themes. */
private val ctaGradientIndigo = Color(0xFF2235A8)

/** Purple stop of [brandCtaButtonGradient] — the brand's CTA gradient, fixed across themes. */
private val ctaGradientPurple = Color(0xFF7C3FC4)

/**
 * The brand gradient behind [com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton].
 * Diagonal (top-left to bottom-right, i.e. CSS `135deg`), via [Offset.Zero]/[Offset.Infinite] so
 * it spans the button regardless of its size. Fixed across themes like [brandTopBarGradient] — a
 * brand asset, not a surface.
 */
private val brandCtaButtonGradient = Brush.linearGradient(
    colors = listOf(ctaGradientIndigo, ctaGradientPurple),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/**
 * Content colour for a [com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton]
 * rendered on top of [brandTopBarGradient] (white container, so this is the text/icon colour).
 * Fixed like [onBrandTopBarGradient], for the same reason.
 */
private val onGradientFilledButton = primaryLight

val lightBrandColors = BrandColors(
    topBarGradient = brandTopBarGradient,
    onTopBarGradient = onBrandTopBarGradient,
    ctaButtonGradient = brandCtaButtonGradient,
    onGradientFilled = onGradientFilledButton,
    tonalButtonContainer = secondaryContainerLight,
    onTonalButtonContainer = onSecondaryContainerLight,
)

// Dark uses a neutral elevated-surface container (surfaceContainerHighDark) with primaryDark as
// content, not secondaryContainer/onSecondaryContainer — secondaryContainerDark (#6B2FA0, opaque
// purple) reads as a different hue family than the bluish primaryDark that
// FlashcardsOutlinedButton and FlashcardsTextButton already use for their dark-theme content, so
// a Tonal button next to either looked inconsistent. Reusing primaryDark directly (not
// onPrimaryContainerDark) keeps the content color pixel-identical to those two, not just
// same-family.
val darkBrandColors = BrandColors(
    topBarGradient = brandTopBarGradient,
    onTopBarGradient = onBrandTopBarGradient,
    ctaButtonGradient = brandCtaButtonGradient,
    onGradientFilled = onGradientFilledButton,
    tonalButtonContainer = surfaceContainerHighDark,
    onTonalButtonContainer = primaryDark,
)

val LocalBrandColors = staticCompositionLocalOf { lightBrandColors }

val MaterialTheme.brandColors: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current
