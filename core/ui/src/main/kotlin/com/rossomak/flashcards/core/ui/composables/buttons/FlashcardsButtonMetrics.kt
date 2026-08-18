package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.rossomak.flashcards.core.ui.theme.AppSizes
import com.rossomak.flashcards.core.ui.theme.AppSpacing
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * The geometry that varies by [FlashcardsButtonSize], resolved once per composition. Icon size
 * isn't here — both tiers share the same M3 [ButtonDefaults.IconSize] (18dp), so it's referenced
 * directly at the call site in [FlashcardsButtonIcon] instead of being duplicated per tier.
 */
internal data class FlashcardsButtonMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val textStyle: TextStyle,
)

@Composable
@ReadOnlyComposable
internal fun FlashcardsButtonSize.metrics(
    sizes: AppSizes = MaterialTheme.sizes,
    spacing: AppSpacing = MaterialTheme.spacing,
    typography: Typography = MaterialTheme.typography,
): FlashcardsButtonMetrics = when (this) {
    FlashcardsButtonSize.Normal -> FlashcardsButtonMetrics(
        height = sizes.buttonHeightNormal,
        horizontalPadding = spacing.medium,
        textStyle = typography.labelLarge,
    )
    FlashcardsButtonSize.Small -> FlashcardsButtonMetrics(
        height = sizes.buttonHeightSmall,
        horizontalPadding = spacing.normal,
        textStyle = typography.labelMedium,
    )
}

/** Container fill alpha for the disabled state on [FlashcardsButtonStyle.Surface] (M3 convention). */
private const val DISABLED_CONTAINER_ALPHA = 0.12f

/** Content (text/icon) alpha for the disabled state on [FlashcardsButtonStyle.Surface] (M3 convention). */
private const val DISABLED_CONTENT_ALPHA = 0.38f

/**
 * Container fill alpha for the disabled state on [FlashcardsButtonStyle.OnGradient]. Same intent
 * as [DISABLED_CONTAINER_ALPHA], based on white instead of `onSurface` since that style sits on
 * the theme-fixed [com.rossomak.flashcards.core.ui.theme.BrandColors.topBarGradient].
 */
private const val ON_GRADIENT_DISABLED_CONTAINER_ALPHA = 0.20f

/** Content alpha for the disabled state on [FlashcardsButtonStyle.OnGradient]. */
private const val ON_GRADIENT_DISABLED_CONTENT_ALPHA = 0.50f

/** Disabled container color for the given [style], shared by every `Flashcards*Button`. */
@Composable
@ReadOnlyComposable
internal fun disabledButtonContainerColorFor(style: FlashcardsButtonStyle): Color = when (style) {
    FlashcardsButtonStyle.Surface -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    FlashcardsButtonStyle.OnGradient -> Color.White.copy(alpha = ON_GRADIENT_DISABLED_CONTAINER_ALPHA)
}

/** Disabled content color for the given [style], shared by every `Flashcards*Button`. */
@Composable
@ReadOnlyComposable
internal fun disabledButtonContentColorFor(style: FlashcardsButtonStyle): Color = when (style) {
    FlashcardsButtonStyle.Surface -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    FlashcardsButtonStyle.OnGradient -> Color.White.copy(alpha = ON_GRADIENT_DISABLED_CONTENT_ALPHA)
}

/**
 * Shared label/icon content for every `Flashcards*Button`, rendered as the `content` lambda of
 * the underlying M3 [androidx.compose.material3.Button]/[androidx.compose.material3.FilledTonalButton]/
 * [androidx.compose.material3.OutlinedButton]/[androidx.compose.material3.TextButton]. Their own
 * internal `Row` already provides icon/label spacing and vertical centering, so this only emits
 * children — it doesn't wrap another `Row`. The icon carries extra padding on its label-facing
 * side, on top of that built-in spacing, because [ButtonDefaults.IconSpacing] alone reads too
 * dense against the icon.
 */
@Composable
internal fun RowScope.FlashcardsButtonContent(
    text: String,
    icon: ImageVector?,
    iconPosition: FlashcardsButtonIconPosition,
    metrics: FlashcardsButtonMetrics,
) {
    val extraIconGap = MaterialTheme.spacing.xxsmall
    if (icon != null && iconPosition == FlashcardsButtonIconPosition.Leading) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize).padding(end = extraIconGap),
        )
    }
    Text(text = text, style = metrics.textStyle)
    if (icon != null && iconPosition == FlashcardsButtonIconPosition.Trailing) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize).padding(start = extraIconGap),
        )
    }
}
