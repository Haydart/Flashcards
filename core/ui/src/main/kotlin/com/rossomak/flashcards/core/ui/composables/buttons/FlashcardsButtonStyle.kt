package com.rossomak.flashcards.core.ui.composables.buttons

/**
 * Which background a `Flashcards*Button` is drawn on — mirrors [MetadataBadgeStyle]. A button
 * cannot read the surface behind it, so the caller declares it.
 */
enum class FlashcardsButtonStyle {
    /** Default — a plain themed surface. */
    Surface,

    /**
     * For use on [com.rossomak.flashcards.core.ui.theme.BrandColors.topBarGradient] (top bars,
     * hero headers). That gradient never flips to a dark variant, so this style's colors are
     * theme-independent.
     */
    OnGradient,
}
