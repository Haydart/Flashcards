// Token-grouping file: AppSizes plus its CompositionLocal and border helper live together,
// matching the sibling Spacing.kt / CornerRadius.kt convention.
@file:Suppress("MatchingDeclarationName")

package com.rossomak.flashcards.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Component dimensions (heights, fixed sizes, border widths) — distinct from [AppSpacing],
 * which owns gaps and padding. Read via [MaterialTheme.sizes] inside composables.
 */
object AppSizes {
    /** Minimum height of a single-line list row (rows grow for multi-line content). */
    val listRowMinHeight: Dp = 64.dp

    /** Rounded, tinted leading icon container used in settings and category rows. */
    val iconTile: Dp = 40.dp

    /** Circular numbered badge used as the leading element of flashcard rows. */
    val numberBadge: Dp = 28.dp

    /** 1px hairline used for card borders and dividers. */
    val hairline: Dp = 1.dp

    /** Height of the checkable tag chip (M3 filter chip, one size only). */
    val tagChipHeight: Dp = 40.dp

    /** Leading check icon inside a selected tag chip. */
    val tagChipIcon: Dp = 18.dp

    /** Outline width of an unselected tag chip — deliberately heavier than [hairline]. */
    val tagChipBorder: Dp = 1.5.dp

    /** Height of the metadata badge — the smaller, non-checkable stats pill. */
    val metadataBadgeHeight: Dp = 32.dp

    /** Leading icon inside a metadata badge. */
    val metadataBadgeIcon: Dp = 16.dp
}

val LocalSizes = staticCompositionLocalOf { AppSizes }

val MaterialTheme.sizes: AppSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalSizes.current

/**
 * The standard 1px hairline border for design-system cards and list groups, tinted with
 * [androidx.compose.material3.ColorScheme.outlineVariant].
 */
val MaterialTheme.hairlineBorder: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() = BorderStroke(width = sizes.hairline, color = colorScheme.outlineVariant)
