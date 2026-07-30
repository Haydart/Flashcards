package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/** Opacity applied to the whole row when `enabled = false`, per the design's disabled rows. */
private const val DISABLED_ALPHA = 0.6f

/**
 * The generic design-system list row: an optional [leading] slot, a title with optional
 * [subtitle] / [secondaryText], and an optional [trailing] slot. Settings, category, and topic
 * rows are all this row composed with different slots (icon tile / play button leading;
 * chevron / switch / stepper trailing).
 *
 * The whole row is one merged, clickable node with the given [role]; decorative trailing
 * chevrons should pass `contentDescription = null` (see [FlashcardsChevron]). When [enabled] is
 * `false`, the whole row dims rather than only disabling the click.
 */
@Composable
fun FlashcardsListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryText: String? = null,
    enabled: Boolean = true,
    role: Role = Role.Button,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .heightIn(min = MaterialTheme.sizes.listRowMinHeight)
            .clickable(enabled = enabled, role = role, onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.normal,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        if (leading != null) {
            leading()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (leading != null) MaterialTheme.spacing.xxsmall else MaterialTheme.spacing.none),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.none),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Decorative drill-in chevron for the [FlashcardsListRow] trailing slot. Its content
 * description is intentionally `null`: the row itself is the labeled, clickable node.
 */
@Composable
fun FlashcardsChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@ShowkaseComposable(name = "List row — nav", group = "Lists")
@Composable
fun FlashcardsListRowShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListRow(
                title = "Android",
                onClick = {},
                subtitle = "Compose · Coroutines · Compose Navigation",
                secondaryText = "13 topics",
                trailing = { FlashcardsChevron() },
            )
        }
    }
}

@ShowkaseComposable(name = "List row — disabled", group = "Lists")
@Composable
fun FlashcardsListRowDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsListRow(
                title = "Speak your answer aloud",
                onClick = {},
                subtitle = "Coming soon",
                enabled = false,
                trailing = { FlashcardsChevron() },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsListRowPreview() {
    FlashcardsTheme {
        Surface {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier
                    .padding(MaterialTheme.spacing.normal)
                    .flashcardsListGroupContainer()
            ) {
                FlashcardsListRow(
                    modifier = Modifier.flashcardsListItemShape(FlashcardsListItemPosition.Top),
                    title = "Android",
                    onClick = {},
                    subtitle = "Compose · Coroutines · Compose Navigation",
                    secondaryText = "13 topics",
                    trailing = { FlashcardsChevron() },
                )
                FlashcardsListRow(
                    modifier = Modifier.flashcardsListItemShape(FlashcardsListItemPosition.Middle),
                    title = "Compose Navigation",
                    onClick = {},
                    subtitle = "in Android",
                    trailing = { FlashcardsChevron() },
                )
                FlashcardsListRow(
                    modifier = Modifier.flashcardsListItemShape(FlashcardsListItemPosition.Bottom),
                    title = "Kotlin Coroutines",
                    onClick = {},
                    subtitle = "Structured concurrency · Flows",
                    secondaryText = "24 cards",
                    leading = {
                        FlashcardsIconTile(
                            icon = Icons.Default.Star,
                            contentDescription = null,
                        )
                    },
                    trailing = { FlashcardsChevron() },
                )
            }
        }
    }
}
