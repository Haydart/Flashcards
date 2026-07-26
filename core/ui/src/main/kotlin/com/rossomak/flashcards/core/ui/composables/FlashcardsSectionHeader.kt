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
 * All-caps, muted section label that precedes a [FlashcardsListGroup] or lazy list section.
 * The caller passes the human-readable [text]; the component applies the uppercase treatment
 * and marks the node as an accessibility heading.
 */
@Composable
fun FlashcardsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .semantics { heading() }
            .padding(
                horizontal = MaterialTheme.spacing.normal,
                vertical = MaterialTheme.spacing.xsmall,
            ),
    )
}

@ShowkaseComposable(name = "Section header", group = "Lists")
@Composable
fun FlashcardsSectionHeaderShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSectionHeader(text = "Study sessions")
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsSectionHeaderPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsSectionHeader(text = "Study sessions")
        }
    }
}
