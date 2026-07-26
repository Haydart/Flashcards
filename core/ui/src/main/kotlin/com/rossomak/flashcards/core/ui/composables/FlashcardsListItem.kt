// Grouping file: the FlashcardsListItemPosition enum and its list-item Modifier helpers ship
// together as one cohesive API.
@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.hairlineBorder
import com.rossomak.flashcards.core.ui.theme.sizes

/**
 * Where a row sits within a grouped list, so a stack of independent `LazyColumn` items reads
 * as one bordered card: only the outer corners round, and interior seams get a divider.
 */
enum class FlashcardsListItemPosition {
    /** The only row in the group — all four corners rounded, no divider. */
    Single,

    /** First row — top corners rounded, divider below. */
    Top,

    /** Interior row — no rounding, divider below. */
    Middle,

    /** Last row — bottom corners rounded, no divider. */
    Bottom,
    ;

    companion object {
        /** Resolves the position of item [index] in a list of [count] items. */
        fun of(index: Int, count: Int): FlashcardsListItemPosition = when {
            count <= 1 -> Single
            index == 0 -> Top
            index == count - 1 -> Bottom
            else -> Middle
        }
    }
}

/**
 * Clips a lazy list item to the right corners for its [position], paints the row surface, and
 * draws the interior divider below non-terminal rows. Pair with [flashcardsListGroupContainer]
 * on the section wrapper to get the single-card border.
 */
@Composable
fun Modifier.flashcardsListItemShape(
    position: FlashcardsListItemPosition,
    showDivider: Boolean = position == FlashcardsListItemPosition.Top ||
        position == FlashcardsListItemPosition.Middle,
): Modifier {
    val corner = MaterialTheme.cornerRadius.card
    val shape = when (position) {
        FlashcardsListItemPosition.Single -> RoundedCornerShape(corner)
        FlashcardsListItemPosition.Top -> RoundedCornerShape(topStart = corner, topEnd = corner)
        FlashcardsListItemPosition.Bottom -> RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
        FlashcardsListItemPosition.Middle -> RectangleShape
    }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val hairlinePx = with(LocalDensity.current) { MaterialTheme.sizes.hairline.toPx() }
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .then(
            if (showDivider) {
                Modifier.drawBehind {
                    val y = size.height - hairlinePx / 2f
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = hairlinePx,
                    )
                }
            } else {
                Modifier
            },
        )
}

/**
 * The single-card border + rounding a screen wraps around its grouped `LazyColumn` section so
 * that individually-shaped [flashcardsListItemShape] rows appear inside one bordered card.
 */
@Composable
fun Modifier.flashcardsListGroupContainer(): Modifier {
    val shape = RoundedCornerShape(MaterialTheme.cornerRadius.card)
    return this
        .clip(shape)
        .border(border = MaterialTheme.hairlineBorder, shape = shape)
}
