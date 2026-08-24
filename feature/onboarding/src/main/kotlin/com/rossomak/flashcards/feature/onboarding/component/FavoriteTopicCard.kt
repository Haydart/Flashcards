package com.rossomak.flashcards.feature.onboarding.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile
import com.rossomak.flashcards.core.ui.theme.FlashcardsMotion
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/** Border width of a favourited card — heavier than the hairline so selection reads at a glance. */
private val SelectedBorderWidth = 2.dp

/** Side of the square selection indicator in the card's top-right corner. */
private val SelectionIndicatorSize = 20.dp

/**
 * A topic the user can favourite during onboarding: a leading category glyph, the topic name, its
 * parent category, and a square selection indicator that fills once picked.
 *
 * A toggleable card rather than a row with a checkbox — the Favorites step lays these out as a
 * two-column grid, and the whole card is the target. One `toggleable` node with [Role.Checkbox], so
 * the indicator is decoration and TalkBack announces the card once.
 */
@Composable
fun FavoriteTopicCard(
    name: String,
    categoryName: String,
    icon: ImageVector,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(FlashcardsMotion.DURATION_SHORT_MS, easing = FlashcardsMotion.StandardEasing),
        label = "favoriteTopicCardBorder",
    )

    Surface(
        modifier = modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = onSelectedChange,
        ),
        shape = RoundedCornerShape(MaterialTheme.cornerRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) SelectedBorderWidth else MaterialTheme.sizes.hairline,
            color = borderColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xsmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                FlashcardsIconTile(icon = icon, contentDescription = null)
                SelectionIndicator(selected = selected)
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = categoryName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.xsmall)
    Surface(
        modifier = Modifier.size(SelectionIndicatorSize),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(MaterialTheme.sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
