package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Compact numeric stepper (−  value  +) used as the trailing control of a settings row. The
 * two buttons are independent tap targets, so each carries its own required content
 * description; the row hosting the stepper must not be clickable itself.
 */
@Composable
fun FlashcardsStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
    ) {
        FilledIconButton(
            onClick = onDecrement,
            enabled = decrementEnabled,
            modifier = Modifier
                .size(MaterialTheme.sizes.iconTile)
                .semantics { contentDescription = decrementContentDescription },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(text = "−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
        )
        FilledIconButton(
            onClick = onIncrement,
            enabled = incrementEnabled,
            modifier = Modifier
                .size(MaterialTheme.sizes.iconTile)
                .semantics { contentDescription = incrementContentDescription },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@ShowkaseComposable(name = "Stepper", group = "Inputs")
@Composable
fun FlashcardsStepperShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsStepper(
                value = 20,
                onDecrement = {},
                onIncrement = {},
                decrementContentDescription = "Fewer cards",
                incrementContentDescription = "More cards",
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsStepperPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsStepper(
                value = 20,
                onDecrement = {},
                onIncrement = {},
                decrementContentDescription = "Fewer cards",
                incrementContentDescription = "More cards",
            )
        }
    }
}
