package com.rossomak.flashcards.core.ui.composables.banners

/**
 * Whether a [FlashcardsXpBreakdownRow] adds or removes XP. Named by direction rather than by color
 * so the enum still reads correctly if the palette changes.
 */
enum class FlashcardsXpBreakdownTone {
    /** XP earned — the translucent white treatment shared with every other on-gradient surface. */
    Gain,

    /** XP lost (de-mastery) — a red-tinted container drawn on the same gradient. */
    Loss,
}
