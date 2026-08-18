package com.rossomak.flashcards.core.ui.composables.buttons

/**
 * Where a `Flashcards*Button`'s optional `icon` renders relative to its label. Buttons carry at
 * most one icon, so this is a single position rather than separate leading/trailing icon slots.
 */
enum class FlashcardsButtonIconPosition {
    Leading,
    Trailing,
}
