package com.rossomak.flashcards.core.ui.composables

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes

/**
 * Circular, tinted play affordance used as a trailing element of topic and search-result rows,
 * beside the chevron (see ADR-0041). It is an independent tap target, distinct from the row's
 * own click, so it carries its own required [contentDescription].
 */
@Composable
fun FlashcardsPlayButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(MaterialTheme.sizes.iconTile),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = contentDescription)
    }
}

@ShowkaseComposable(name = "Play button", group = "Lists")
@Composable
fun FlashcardsPlayButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsPlayButton(onClick = {}, contentDescription = "Study Compose")
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsPlayButtonPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsPlayButton(onClick = {}, contentDescription = "Study Compose")
        }
    }
}
