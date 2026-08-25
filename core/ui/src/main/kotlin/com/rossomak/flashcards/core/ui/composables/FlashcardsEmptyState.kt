package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.FlashcardsEmptyStateTone.Error
import com.rossomak.flashcards.core.ui.composables.FlashcardsEmptyStateTone.Info
import com.rossomak.flashcards.core.ui.composables.buttons.FlashcardsFilledButton
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.semanticColors
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * The shared layout for "nothing to show" and "something went wrong" screens: a tinted, color-coded
 * icon circle, a title, up to two lines of supporting copy, and an optional [FlashcardsFilledButton]
 * CTA. Used for empty search results, empty lists, cleared filters, and load errors alike — [tone]
 * is the only thing that shifts between "empty" and "error".
 *
 * Does not size or center itself — pass a `modifier` that does (`Modifier.fillMaxSize()` plus
 * whatever alignment the call site needs), same as every other `core:ui` composable that doesn't
 * own its own layout slot.
 *
 * [ctaLabel] and [onCtaClick] gate the CTA together: the button only renders when both are
 * non-null, so passing one without the other silently omits the button rather than crashing.
 */
@Composable
fun FlashcardsEmptyState(
    icon: ImageVector,
    title: String,
    supportingText: String,
    modifier: Modifier = Modifier,
    tone: FlashcardsEmptyStateTone = Info,
    ctaLabel: String? = null,
    ctaIcon: ImageVector? = null,
    onCtaClick: (() -> Unit)? = null,
) {
    val semanticColors = MaterialTheme.semanticColors
    val (containerColor, contentColor) = when (tone) {
        Info -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        Error -> semanticColors.negativeContainer to semanticColors.onNegativeContainer
    }

    Column(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.sizes.emptyStateIconCircle)
                .background(color = containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                // Decorative: the title below already names the state for a screen reader.
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(MaterialTheme.sizes.emptyStateIcon),
            )
        }

        Box(modifier = Modifier.padding(top = MaterialTheme.spacing.normal)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        Box(modifier = Modifier.padding(top = MaterialTheme.spacing.xsmall)) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (ctaLabel != null && onCtaClick != null) {
            Box(modifier = Modifier.padding(top = MaterialTheme.spacing.normal)) {
                FlashcardsFilledButton(text = ctaLabel, onClick = onCtaClick, icon = ctaIcon)
            }
        }
    }
}

@ShowkaseComposable(name = "Empty state — Info", group = "Empty states")
@PreviewLightDark
@Composable
private fun FlashcardsEmptyStateInfoShowcase() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.padding(MaterialTheme.spacing.normal)) {
                FlashcardsEmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No matches",
                    supportingText = "Nothing matches \"xyz123\". Try a different search.",
                )
            }
        }
    }
}

@ShowkaseComposable(name = "Empty state — with CTA", group = "Empty states")
@PreviewLightDark
@Composable
private fun FlashcardsEmptyStateWithCtaShowcase() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.padding(MaterialTheme.spacing.normal)) {
                FlashcardsEmptyState(
                    icon = Icons.Filled.School,
                    title = "Ready to start?",
                    supportingText = "Run your first session — recents and favorites will appear here.",
                    ctaLabel = "Start",
                    ctaIcon = Icons.Filled.School,
                    onCtaClick = {},
                )
            }
        }
    }
}

@ShowkaseComposable(name = "Empty state — Error", group = "Empty states")
@PreviewLightDark
@Composable
private fun FlashcardsEmptyStateErrorShowcase() {
    FlashcardsTheme {
        Surface {
            Box(modifier = Modifier.padding(MaterialTheme.spacing.normal)) {
                FlashcardsEmptyState(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Something went wrong",
                    supportingText = "Couldn't load your categories. Check your connection and try again.",
                    tone = Error,
                    ctaLabel = "Retry",
                    onCtaClick = {},
                )
            }
        }
    }
}
