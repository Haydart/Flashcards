package com.rossomak.flashcards.core.ui.composables.common

/**
 * Size tier shared across component families (buttons, progress bars, …). Each family resolves
 * its own geometry per tier — e.g. `FlashcardsButtonMetrics.kt`, `FlashcardsProgressBarMetrics.kt`
 * — rather than sharing concrete dimensions, only the two-tier axis itself. See ADR-0034.
 */
enum class FlashcardsComponentSize {
    /** The default, canonical tier. */
    Normal,

    /** Compact/dense contexts such as inline row actions or list rows. */
    Small,
}
