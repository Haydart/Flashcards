package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * All-caps, muted overline label used as a header of list sections, dashboard stat cards, and form
 * field hints. The caller passes the human-readable [text]; the component applies the uppercase
 * treatment and, unless [isHeading] is false, marks the node as an accessibility heading.
 */
@Composable
fun FlashcardsOverlineLabel(
    text: String,
    modifier: Modifier = Modifier,
    isHeading: Boolean = true,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .then(if (isHeading) Modifier.semantics { heading() } else Modifier)
            .padding(
                horizontal = MaterialTheme.spacing.normal,
                vertical = MaterialTheme.spacing.xsmall,
            ),
    )
}

@ShowkaseComposable(name = "Overline label", group = "Labels")
@Composable
fun FlashcardsOverlineLabelShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsOverlineLabel(text = "Study sessions")
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsOverlineLabelPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsOverlineLabel(text = "Study sessions")
        }
    }
}
