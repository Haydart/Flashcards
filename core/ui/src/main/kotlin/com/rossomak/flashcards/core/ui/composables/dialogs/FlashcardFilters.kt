package com.rossomak.flashcards.core.ui.composables.dialogs

/**
 * The two facets a session can be filtered by, kept together because they are always read, edited
 * and discarded as one unit — and because a dialog's draft is a single value, not a scattering of
 * fields.
 *
 * @param selectedTags OR-within: a card matches if it carries any selected tag. Empty means "all".
 * @param difficultyRange AND-combined with [selectedTags].
 */
data class FlashcardFilters(
    val selectedTags: Set<String>,
    val difficultyRange: IntRange,
)

/**
 * Toggles one tag's membership.
 *
 * Lives here rather than in a dialog host because a host may only do total field-level `copy()`
 * (ADR-0036) — set arithmetic in a composable would be logic no unit test can reach.
 */
fun FlashcardFilters.withTag(tag: String, isSelected: Boolean): FlashcardFilters =
    copy(selectedTags = if (isSelected) selectedTags + tag else selectedTags - tag)
