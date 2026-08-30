package com.rossomak.flashcards.core.domain.model

import kotlinx.serialization.Serializable

/**
 * How a set of Flashcards is ordered for the user.
 *
 * **One notion, not two** (ADR-0038): the order a Subcategory's Flashcards are listed in while
 * browsing and the order they are presented in during a Study Session are the same concept, stored
 * once as the user's saved preference and edited from Settings, Subcategory Details and the Preview
 * Study Session screen alike.
 */
@Serializable
enum class FlashcardSortOrder {

    /**
     * No explicit Difficulty ordering — so each context falls back to its own natural order, and
     * this one value has two realizations by design. Browsing a Subcategory, it is the order the
     * repository returned. Inside a Study Session, it is the order the seeded draw produced, since
     * the shuffle has already happened by the time ordering applies.
     */
    Default,

    EasiestFirst,

    HardestFirst,
}
