package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
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
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/** Container fill alpha for [FlashcardsButtonStyle.OnGradient]'s translucent white pill. */
private const val ON_GRADIENT_CONTAINER_ALPHA = 0.18f

/** Border alpha for [FlashcardsButtonStyle.OnGradient]'s translucent white pill. */
private const val ON_GRADIENT_BORDER_ALPHA = 0.35f

/**
 * The secondary-emphasis button — a tinted fill without the [FlashcardsFilledButton]'s gradient
 * weight ("Edit", "Save changes"). Use where an action matters but shouldn't compete with the
 * screen's primary CTA.
 */
@Composable
fun FlashcardsTonalButton(
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
        containerColor = if (onGradient) {
            Color.White.copy(alpha = ON_GRADIENT_CONTAINER_ALPHA)
        } else {
            MaterialTheme.brandColors.tonalButtonContainer
        },
        contentColor = if (onGradient) MaterialTheme.brandColors.onTopBarGradient else MaterialTheme.brandColors.onTonalButtonContainer,
        border = if (onGradient) {
            BorderStroke(MaterialTheme.sizes.tagChipBorder, Color.White.copy(alpha = ON_GRADIENT_BORDER_ALPHA))
        } else {
            null
        },
        disabledContainerColor = disabledButtonContainerColorFor(style),
        disabledContentColor = disabledButtonContentColorFor(style),
    )
}

@ShowkaseComposable(name = "Tonal", group = "Buttons")
@Composable
fun FlashcardsTonalButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTonalButton(text = "Edit", onClick = {}, icon = Icons.Default.Edit)
        }
    }
}

@ShowkaseComposable(name = "Tonal — small", group = "Buttons")
@Composable
fun FlashcardsTonalButtonSmallShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTonalButton(text = "Edit", onClick = {}, size = FlashcardsButtonSize.Small, icon = Icons.Default.Edit)
        }
    }
}

@ShowkaseComposable(name = "Tonal — disabled", group = "Buttons")
@Composable
fun FlashcardsTonalButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTonalButton(text = "Edit", onClick = {}, enabled = false)
        }
    }
}

@ShowkaseComposable(name = "Tonal — on gradient", group = "Buttons")
@Preview
@Composable
fun FlashcardsTonalButtonOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.topBarGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            FlashcardsTonalButton(
                text = "Play audio",
                onClick = {},
                icon = Icons.Default.PlayArrow,
                style = FlashcardsButtonStyle.OnGradient,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsTonalButtonPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsTonalButton(text = "Edit", onClick = {}, icon = Icons.Default.Edit)
        }
    }
}
