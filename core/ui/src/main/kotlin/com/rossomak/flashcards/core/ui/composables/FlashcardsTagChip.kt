package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Small, non-interactive pill label — the flat study tags shown on flashcard rows. Display
 * only; selection/filter behaviour lives in the calling screen.
 */
@Composable
fun FlashcardsTagChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(MaterialTheme.cornerRadius.small),
            )
            .padding(
                horizontal = MaterialTheme.spacing.xsmall,
                vertical = MaterialTheme.spacing.xxsmall,
            ),
    )
}

@ShowkaseComposable(name = "Tag chip", group = "Lists")
@Composable
fun FlashcardsTagChipShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsTagChip(label = "State")
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsTagChipPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsTagChip(label = "State")
        }
    }
}
