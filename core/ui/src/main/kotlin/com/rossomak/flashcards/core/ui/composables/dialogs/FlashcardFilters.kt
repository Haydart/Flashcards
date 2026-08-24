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
