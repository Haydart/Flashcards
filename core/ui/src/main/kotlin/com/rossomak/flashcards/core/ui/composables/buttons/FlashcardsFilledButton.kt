package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Container fill alpha for [com.rossomak.flashcards.core.ui.theme.BrandColors.ctaButtonGradient]
 * when disabled. M3's `ButtonColors.disabledContainerColor` only takes a flat [Color], so the
 * gradient itself keeps painting (via a background modifier behind a transparent [Button]) and is
 * dimmed with this alpha instead of being swapped out for a solid fill.
 */
private const val DISABLED_GRADIENT_ALPHA = 0.38f

/**
 * The primary CTA button — filled with [com.rossomak.flashcards.core.ui.theme.BrandColors.ctaButtonGradient].
 * One per screen, reserved for the single most important action ("Start studying", "New deck").
 */
@Composable
fun FlashcardsFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: FlashcardsButtonSize = FlashcardsButtonSize.Normal,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconPosition: FlashcardsButtonIconPosition = FlashcardsButtonIconPosition.Leading,
    style: FlashcardsButtonStyle = FlashcardsButtonStyle.Surface,
) {
    val onGradient = style == FlashcardsButtonStyle.OnGradient
    val metrics = size.metrics()
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.full)
    val gradientModifier = if (onGradient) {
        Modifier
    } else {
        Modifier.background(
            brush = MaterialTheme.brandColors.ctaButtonGradient,
            shape = shape,
            alpha = if (enabled) 1f else DISABLED_GRADIENT_ALPHA,
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(metrics.height).then(gradientModifier),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (onGradient) Color.White else Color.Transparent,
            // Fixed white on both branches, not colorScheme.onPrimary — the Surface-style
            // container is the ctaButtonGradient brush, which is itself fixed across themes (see
            // BrandColors.kt), so its content color must be fixed too.
            contentColor = if (onGradient) MaterialTheme.brandColors.onGradientFilled else Color.White,
            disabledContainerColor = if (onGradient) disabledButtonContainerColorFor(style) else Color.Transparent,
            disabledContentColor = disabledButtonContentColorFor(style),
        ),
        contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 0.dp),
    ) {
        FlashcardsButtonContent(text = text, icon = icon, iconPosition = iconPosition, metrics = metrics)
    }
}

@ShowkaseComposable(name = "Filled", group = "Buttons")
@Composable
fun FlashcardsFilledButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsFilledButton(text = "Start studying", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Filled — with icon", group = "Buttons")
@Composable
fun FlashcardsFilledButtonWithIconShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsFilledButton(text = "New deck", onClick = {}, icon = Icons.Default.Add)
        }
    }
}

@ShowkaseComposable(name = "Filled — small", group = "Buttons")
@Composable
fun FlashcardsFilledButtonSmallShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsFilledButton(text = "Add card", onClick = {}, size = FlashcardsButtonSize.Small, icon = Icons.Default.Add)
        }
    }
}

@ShowkaseComposable(name = "Filled — disabled", group = "Buttons")
@Composable
fun FlashcardsFilledButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsFilledButton(text = "Start studying", onClick = {}, enabled = false)
        }
    }
}

@ShowkaseComposable(name = "Filled — on gradient", group = "Buttons")
@Preview
@Composable
fun FlashcardsFilledButtonOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.topBarGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                FlashcardsFilledButton(
                    text = "Study now",
                    onClick = {},
                    icon = Icons.Default.School,
                    style = FlashcardsButtonStyle.OnGradient,
                )
                FlashcardsFilledButton(
                    text = "Study now",
                    onClick = {},
                    icon = Icons.Default.School,
                    style = FlashcardsButtonStyle.OnGradient,
                    enabled = false,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsFilledButtonPreview() {
    FlashcardsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            ) {
                FlashcardsFilledButton(text = "New deck", onClick = {}, icon = Icons.Default.Add)
                FlashcardsFilledButton(text = "New deck", onClick = {}, icon = Icons.Default.Add, enabled = false)
            }
        }
    }
}
