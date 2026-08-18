package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.spacing

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
    FlashcardsButtonLayout(
        text = text,
        onClick = onClick,
        size = size,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        iconPosition = iconPosition,
        containerColor = if (onGradient) Color.White else Color.Transparent,
        containerBrush = if (onGradient) null else MaterialTheme.brandColors.ctaButtonGradient,
        // Fixed white on both branches, not colorScheme.onPrimary — the Surface-style container
        // is MaterialTheme.brandColors.ctaButtonGradient, which is itself fixed across themes
        // (see BrandColors.kt), so its content color must be fixed too.
        contentColor = if (onGradient) MaterialTheme.brandColors.onGradientFilled else Color.White,
        disabledContainerColor = disabledButtonContainerColorFor(style),
        disabledContentColor = disabledButtonContentColorFor(style),
    )
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
            FlashcardsFilledButton(
                text = "Study now",
                onClick = {},
                icon = Icons.Default.School,
                style = FlashcardsButtonStyle.OnGradient,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsFilledButtonPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsFilledButton(text = "New deck", onClick = {}, icon = Icons.Default.Add)
        }
    }
}
