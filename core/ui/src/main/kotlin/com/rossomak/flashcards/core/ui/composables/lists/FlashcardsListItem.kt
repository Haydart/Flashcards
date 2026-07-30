@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.composables.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.sizes

/**
 * Where a row sits within a grouped list, so a stack of independent `LazyColumn` items reads
 * as one card: only the outer corners round, and interior seams get a 1dp gap.
 */
enum class FlashcardsListItemPosition {

    Single, // The only row in the group — all four corners rounded, no gap
    Top, // First row — top corners rounded, gap below
    Middle, // Interior row — no rounding, gap below
    Bottom; // Last row — bottom corners rounded, no gap

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
 * Clips a lazy list item to the right corners for its [position] and, unless [checked], paints
 * the row surface inset by a 1dp gap at the bottom (skipped on [Bottom]/[Single], which have no
 * following row). The gap is just [flashcardsListGroupContainer]'s background showing through —
 * there's no divider drawn. It's always reserved by position, regardless of [checked]: a
 * [checked] row (multi-select mode) paints no surface at all, so the gap is invisible wherever
 * either neighbor is also checked, without needing to look at neighboring rows.
 */
@Composable
fun Modifier.flashcardsListItemShape(
    position: FlashcardsListItemPosition,
    checked: Boolean = false,
    showGap: Boolean = position == FlashcardsListItemPosition.Top || position == FlashcardsListItemPosition.Middle,
): Modifier {
    val corner = MaterialTheme.cornerRadius.card
    val shape = when (position) {
        FlashcardsListItemPosition.Single -> RoundedCornerShape(corner)
        FlashcardsListItemPosition.Top -> RoundedCornerShape(topStart = corner, topEnd = corner)
        FlashcardsListItemPosition.Bottom -> RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
        FlashcardsListItemPosition.Middle -> RectangleShape
    }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val hairlinePx = with(LocalDensity.current) { MaterialTheme.sizes.hairline.toPx() }
    return this
        .clip(shape)
        .then(
            if (checked) {
                Modifier
            } else {
                Modifier.drawBehind {
                    val height = if (showGap) size.height - hairlinePx else size.height
                    drawRect(color = surfaceColor, size = Size(size.width, height))
                }
            },
        )
}

/**
 * The group background a screen wraps around its grouped `LazyColumn` section, so individually
 * shaped [flashcardsListItemShape] rows appear inside one rounded card and the 1dp gaps between
 * unchecked rows show this color rather than a drawn divider line. Insets horizontally first, so
 * the screen behind shows through on every side of the rounded card, not just top and bottom.
 */
@Composable
fun Modifier.flashcardsListGroupContainer(): Modifier = this
    .clip(RoundedCornerShape(MaterialTheme.cornerRadius.card))
    .background(MaterialTheme.colorScheme.surfaceContainerLow)
