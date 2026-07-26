package com.rossomak.flashcards.core.ui.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsMotion
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * A multi-select list row: a leading checkbox, a title with optional [subtitle], and a
 * selection tint that animates in when [selected]. The whole row is one toggleable node with
 * `Role.Checkbox`, so the inner [Checkbox] takes `onCheckedChange = null` and TalkBack
 * announces the row as a single checked/unchecked control.
 */
@Composable
fun FlashcardsSelectableListRow(
    title: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(FlashcardsMotion.DURATION_SHORT_MS, easing = FlashcardsMotion.StandardEasing),
        label = "selectionTint",
    )
    Row(
        modifier = modifier
            .heightIn(min = MaterialTheme.sizes.listRowMinHeight)
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onSelectedChange,
            )
            .background(backgroundColor)
            .padding(
                horizontal = MaterialTheme.spacing.normal,
                vertical = MaterialTheme.spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
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
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@ShowkaseComposable(name = "List row — selectable, checked", group = "Lists")
@Composable
fun FlashcardsSelectableListRowCheckedShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSelectableListRow(
                title = "Compose",
                selected = true,
                onSelectedChange = {},
                subtitle = "80 cards",
                trailing = { FlashcardsChevron() },
            )
        }
    }
}

@ShowkaseComposable(name = "List row — selectable, unchecked", group = "Lists")
@Composable
fun FlashcardsSelectableListRowUncheckedShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsSelectableListRow(
                title = "Testing",
                selected = false,
                onSelectedChange = {},
                subtitle = "25 cards",
                trailing = { FlashcardsChevron() },
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsSelectableListRowPreview() {
    FlashcardsTheme {
        Surface {
            Column {
                FlashcardsSelectableListRow(
                    title = "Compose",
                    selected = true,
                    onSelectedChange = {},
                    subtitle = "80 cards",
                    trailing = { FlashcardsChevron() },
                )
                FlashcardsSelectableListRow(
                    title = "Testing",
                    selected = false,
                    onSelectedChange = {},
                    subtitle = "25 cards",
                    trailing = { FlashcardsChevron() },
                )
            }
        }
    }
}
