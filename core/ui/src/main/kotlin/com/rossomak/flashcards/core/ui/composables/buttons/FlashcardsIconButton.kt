package com.rossomak.flashcards.core.ui.composables.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentSize
import com.rossomak.flashcards.core.ui.composables.common.FlashcardsComponentStyle
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * The family's icon-only type — a circular, tonal affordance with no label to carry meaning, so
 * [contentDescription] is required rather than optional. Wraps M3's [FilledIconButton] directly
 * for its touch target, state layer and press behaviour, the same reasoning ADR-0033 applied to
 * the labeled types. Tonal treatment only — both known callers (row play affordances) are tonal,
 * so a `variant` axis spanning filled/outlined/text would invent combinations nobody has designed.
 * See ADR-0042.
 */
@Composable
fun FlashcardsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: FlashcardsComponentSize = FlashcardsComponentSize.Normal,
    enabled: Boolean = true,
    style: FlashcardsComponentStyle = FlashcardsComponentStyle.OnSurface,
) {
    val metrics = size.metrics()

    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(metrics.height),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = enabledButtonContainerColorFor(style, MaterialTheme.colorScheme.secondaryContainer),
            contentColor = enabledButtonContentColorFor(style, MaterialTheme.colorScheme.onSecondaryContainer),
            disabledContainerColor = disabledButtonContainerColorFor(style),
            disabledContentColor = disabledButtonContentColorFor(style),
        ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@ShowkaseComposable(name = "Icon", group = "Buttons")
@Composable
fun FlashcardsIconButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsIconButton(icon = Icons.Default.PlayArrow, contentDescription = "Study Compose", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Icon — small", group = "Buttons")
@Composable
fun FlashcardsIconButtonSmallShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsIconButton(
                icon = Icons.Default.PlayArrow,
                contentDescription = "Study Compose",
                onClick = {},
                size = FlashcardsComponentSize.Small,
            )
        }
    }
}

@ShowkaseComposable(name = "Icon — disabled", group = "Buttons")
@Composable
fun FlashcardsIconButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsIconButton(icon = Icons.Default.PlayArrow, contentDescription = "Study Compose", onClick = {}, enabled = false)
        }
    }
}

@ShowkaseComposable(name = "Icon — on gradient", group = "Buttons")
@Preview
@Composable
fun FlashcardsIconButtonOnGradientShowcase() {
    FlashcardsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.brandColors.topBarGradient)
                .padding(MaterialTheme.spacing.small),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall)) {
                FlashcardsIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Study Compose",
                    onClick = {},
                    style = FlashcardsComponentStyle.OnGradient,
                )
                FlashcardsIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Study Compose",
                    onClick = {},
                    style = FlashcardsComponentStyle.OnGradient,
                    enabled = false,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsIconButtonPreview() {
    FlashcardsTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
            ) {
                FlashcardsIconButton(icon = Icons.Default.PlayArrow, contentDescription = "Study Compose", onClick = {})
                FlashcardsIconButton(icon = Icons.Default.PlayArrow, contentDescription = "Study Compose", onClick = {}, enabled = false)
            }
        }
    }
}
