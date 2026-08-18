package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
 * The lowest-emphasis button — no fill, no border, text (+ optional icon) only ("Skip",
 * "Learn more"). Use for a dismissive or auxiliary action that shouldn't draw the eye.
 */
@Composable
fun FlashcardsTextButton(
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
        contentColor = if (onGradient) MaterialTheme.brandColors.onTopBarGradient else MaterialTheme.colorScheme.primary,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = disabledButtonContentColorFor(style),
    )
}

@ShowkaseComposable(name = "Text", group = "Buttons")
@Composable
fun FlashcardsTextButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTextButton(text = "Skip", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Text — with icon", group = "Buttons")
@Composable
fun FlashcardsTextButtonWithIconShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTextButton(text = "Learn more", onClick = {}, icon = Icons.Default.Info)
        }
    }
}

@ShowkaseComposable(name = "Text — small", group = "Buttons")
@Composable
fun FlashcardsTextButtonSmallShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTextButton(text = "Skip", onClick = {}, size = FlashcardsButtonSize.Small)
        }
    }
}

@ShowkaseComposable(name = "Text — disabled", group = "Buttons")
@Composable
fun FlashcardsTextButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTextButton(text = "Skip", onClick = {}, enabled = false)
        }
    }
}

@ShowkaseComposable(name = "Text — on gradient", group = "Buttons")
@Preview
@Composable
fun FlashcardsTextButtonOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.topBarGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            FlashcardsTextButton(text = "Not sure", onClick = {}, style = FlashcardsButtonStyle.OnGradient)
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsTextButtonPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsTextButton(text = "Learn more", onClick = {}, icon = Icons.Default.Info)
        }
    }
}
