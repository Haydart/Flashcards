package com.rossomak.flashcards.core.ui.composables.buttons

/**
 * Size tier shared by every `Flashcards*Button` composable. Backs onto
 * [com.rossomak.flashcards.core.ui.theme.AppSizes] for height/icon-size and
 * [com.rossomak.flashcards.core.ui.theme.AppSpacing] for content padding/icon-label gap.
 */
enum class FlashcardsButtonSize {
    /** 56dp — the default, canonical CTA height. */
    Normal,

    /** 40dp — compact/dense contexts such as inline row actions. */
    Small,
}
