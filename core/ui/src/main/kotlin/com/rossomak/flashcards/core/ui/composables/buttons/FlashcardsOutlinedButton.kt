package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/** Border alpha for [FlashcardsButtonStyle.OnGradient]'s white outline. */
private const val ON_GRADIENT_BORDER_ALPHA = 0.55f

/**
 * The border-only button — transparent fill, [MaterialTheme.colorScheme.primary] border and
 * content ("Cancel", "Share"). Use for a secondary action that should read lighter than
 * [FlashcardsTonalButton] but still show a defined tap target.
 */
@Composable
fun FlashcardsOutlinedButton(
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
    val contentColor = if (onGradient) MaterialTheme.brandColors.onTopBarGradient else MaterialTheme.colorScheme.primary
    val metrics = size.metrics()

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(metrics.height),
        enabled = enabled,
        shape = RoundedCornerShape(MaterialTheme.cornerRadius.full),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = disabledButtonContentColorFor(style),
        ),
        border = if (enabled) {
            BorderStroke(
                width = MaterialTheme.sizes.tagChipBorder,
                color = if (onGradient) Color.White.copy(alpha = ON_GRADIENT_BORDER_ALPHA) else contentColor,
            )
        } else {
            BorderStroke(MaterialTheme.sizes.tagChipBorder, disabledButtonContentColorFor(style))
        },
        contentPadding = PaddingValues(horizontal = metrics.horizontalPadding, vertical = 0.dp),
    ) {
        FlashcardsButtonContent(text = text, icon = icon, iconPosition = iconPosition, metrics = metrics)
    }
}

@ShowkaseComposable(name = "Outlined", group = "Buttons")
@Composable
fun FlashcardsOutlinedButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsOutlinedButton(text = "Cancel", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Outlined — with icon", group = "Buttons")
@Composable
fun FlashcardsOutlinedButtonWithIconShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsOutlinedButton(text = "Share", onClick = {}, icon = Icons.Default.Share)
        }
    }
}

@ShowkaseComposable(name = "Outlined — small", group = "Buttons")
@Composable
fun FlashcardsOutlinedButtonSmallShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsOutlinedButton(text = "Cancel", onClick = {}, size = FlashcardsButtonSize.Small)
        }
    }
}

@ShowkaseComposable(name = "Outlined — disabled", group = "Buttons")
@Composable
fun FlashcardsOutlinedButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsOutlinedButton(text = "Cancel", onClick = {}, enabled = false)
        }
    }
}

@ShowkaseComposable(name = "Outlined — on gradient", group = "Buttons")
@Preview
@Composable
fun FlashcardsOutlinedButtonOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.topBarGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            FlashcardsOutlinedButton(text = "Skip card", onClick = {}, style = FlashcardsButtonStyle.OnGradient)
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsOutlinedButtonPreview() {
    FlashcardsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            ) {
                FlashcardsOutlinedButton(text = "Share", onClick = {}, icon = Icons.Default.Share)
                FlashcardsOutlinedButton(text = "Share", onClick = {}, icon = Icons.Default.Share, enabled = false)
            }
        }
    }
}
