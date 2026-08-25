package com.rossomak.flashcards.core.ui.composables

/**
 * Color coding of a [FlashcardsEmptyState]'s icon circle. Distinct from
 * [com.rossomak.flashcards.core.ui.composables.banners.FlashcardsInfoBannerTone] despite the
 * shared "Info" name — that enum's `Info` is amber, this one's is purple, so the two are not
 * interchangeable.
 */
enum class FlashcardsEmptyStateTone {
    /** Purple — nothing is wrong yet, e.g. "no results", "nothing here yet". The default. */
    Info,

    /** Red — something failed and needs the reader's attention, e.g. a load error. */
    Error,
}
