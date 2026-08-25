package com.rossomak.flashcards.feature.onboarding.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes
import com.rossomak.flashcards.core.ui.theme.spacing

/**
 * Side of this card's leading glyph tile — smaller than the shared
 * [com.rossomak.flashcards.core.ui.composables.FlashcardsIconTile] (40dp): the grid packs six-plus
 * cards on screen at once, so the standard tile reads oversized here.
 */
private val TopicIconTileSize = 28.dp

/**
 * A topic the user can favourite during onboarding: a leading category glyph, the topic name, its
 * parent category, and a trailing bookmark that fills once picked.
 *
 * Unselected cards sit in the same translucent-on-gradient treatment as
 * [com.rossomak.flashcards.core.ui.composables.banners.FlashcardsInfoBanner]'s `OnGradient` style —
 * they read as glass panes over the brand background. A picked card switches to an opaque themed
 * surface so it visibly pops off the grid instead of only gaining a border.
 *
 * A toggleable card rather than a row with a checkbox — the Favorites step lays these out as a
 * two-column grid, and the whole card is the target. One `toggleable` node with [Role.Checkbox], so
 * the bookmark is decoration and TalkBack announces the card once.
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
    val brandColors = MaterialTheme.brandColors

    Surface(
        modifier = modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = onSelectedChange,
        ),
        shape = RoundedCornerShape(MaterialTheme.cornerRadius.card),
        color = if (selected) MaterialTheme.colorScheme.surface else brandColors.onGradientContainer,
        contentColor = if (selected) MaterialTheme.colorScheme.onSurface else brandColors.onGradientContent,
        border = if (selected) {
            null
        } else {
            BorderStroke(MaterialTheme.sizes.onGradientBorder, brandColors.onGradientBorder)
        },
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.xsmall),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxsmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (selected) {
                    TopicIconTile(icon = icon)
                } else {
                    TopicIconTile(
                        icon = icon,
                        contentColor = brandColors.onGradientContent,
                        containerColor = brandColors.onGradientBorder,
                    )
                }
                Icon(
                    imageVector = if (selected) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.sizes.metadataBadgeIcon),
                    tint = if (selected) MaterialTheme.colorScheme.primary else brandColors.onGradientContent,
                )
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
                color = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else brandColors.onGradientContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A [TopicIconTileSize] glyph tile — see that constant for why this isn't [FlashcardsIconTile]. */
@Composable
private fun TopicIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor: Color = contentColor.copy(alpha = DEFAULT_CONTAINER_ALPHA),
) {
    Box(
        modifier = modifier
            .size(TopicIconTileSize)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(MaterialTheme.cornerRadius.small),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.sizes.metadataBadgeIcon),
            )
        }
    }
}

/** Opacity of [MaterialTheme.colorScheme.secondaryContainer] used as [TopicIconTile]'s default fill. */
private const val DEFAULT_CONTAINER_ALPHA = 0.12f
