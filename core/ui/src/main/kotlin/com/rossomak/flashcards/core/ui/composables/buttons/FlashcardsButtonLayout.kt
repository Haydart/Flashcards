package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.rossomak.flashcards.core.ui.theme.AppSizes
import com.rossomak.flashcards.core.ui.theme.AppSpacing
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/** The geometry that varies by [FlashcardsButtonSize], resolved once per composition. */
private data class FlashcardsButtonMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

@Composable
@ReadOnlyComposable
private fun FlashcardsButtonSize.metrics(
    sizes: AppSizes = MaterialTheme.sizes,
    spacing: AppSpacing = MaterialTheme.spacing,
    typography: Typography = MaterialTheme.typography,
): FlashcardsButtonMetrics = when (this) {
    FlashcardsButtonSize.Normal -> FlashcardsButtonMetrics(
        height = sizes.buttonHeightNormal,
        horizontalPadding = spacing.medium,
        iconSize = sizes.buttonIconSizeNormal,
        textStyle = typography.labelLarge,
    )
    FlashcardsButtonSize.Small -> FlashcardsButtonMetrics(
        height = sizes.buttonHeightSmall,
        horizontalPadding = spacing.normal,
        iconSize = sizes.buttonIconSizeSmall,
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
 * Shared geometry + layout for every `Flashcards*Button`. Built on [Surface] rather than M3's
 * [androidx.compose.material3.Button]/[androidx.compose.material3.TextButton] family, which
 * enforce a minimum touch-target height that would clip [FlashcardsButtonSize.Small] back up to
 * 40dp; [Surface] gives click handling, ripple, and disabled semantics without that constraint.
 *
 * Only [FlashcardsFilledButton] passes [containerBrush] (a gradient can't be expressed as a
 * plain [Surface] `color`), so it layers the brush on as a background instead and leaves the
 * `Surface` itself transparent.
 */
@Composable
internal fun FlashcardsButtonLayout(
    text: String,
    onClick: () -> Unit,
    size: FlashcardsButtonSize,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconPosition: FlashcardsButtonIconPosition = FlashcardsButtonIconPosition.Leading,
    containerColor: Color = Color.Transparent,
    containerBrush: Brush? = null,
    disabledContainerColor: Color = disabledButtonContainerColorFor(FlashcardsButtonStyle.Surface),
    disabledContentColor: Color = disabledButtonContentColorFor(FlashcardsButtonStyle.Surface),
    border: BorderStroke? = null,
) {
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.full)
    val metrics = size.metrics()
    val showsGradientFill = containerBrush != null && enabled
    val surfaceModifier = modifier.height(metrics.height).then(
        if (showsGradientFill) Modifier.background(brush = containerBrush, shape = shape) else Modifier,
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = surfaceModifier,
        shape = shape,
        color = if (showsGradientFill) {
            Color.Transparent
        } else if (enabled) {
            containerColor
        } else {
            disabledContainerColor
        },
        contentColor = if (enabled) contentColor else disabledContentColor,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlashcardsButtonIcon(icon, FlashcardsButtonIconPosition.Leading, iconPosition, metrics.iconSize)
            Text(text = text, style = metrics.textStyle)
            FlashcardsButtonIcon(icon, FlashcardsButtonIconPosition.Trailing, iconPosition, metrics.iconSize)
        }
    }
}

/** Renders [icon] only when it's set and [wantedPosition] matches the button's [actualPosition]. */
@Composable
private fun FlashcardsButtonIcon(
    icon: ImageVector?,
    wantedPosition: FlashcardsButtonIconPosition,
    actualPosition: FlashcardsButtonIconPosition,
    iconSize: Dp,
) {
    if (icon != null && actualPosition == wantedPosition) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
    }
}
