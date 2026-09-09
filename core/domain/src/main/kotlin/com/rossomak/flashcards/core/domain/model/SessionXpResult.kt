package com.rossomak.flashcards.core.domain.model

/**
 * [com.rossomak.flashcards.core.domain.usecase.CalculateSessionXpUseCase]'s whole result: what a
 * session earned, the account's [newScoringState] after applying it, and every level [levelsCrossed]
 * along the way, in ascending order (a future celebration consumes them one at a time; this
 * type only has to report them correctly).
 *
 * [newCardsStudied] rides along because it is the one input behind [breakdown]'s numbers that isn't
 * itself part of [breakdown] or [newScoringState] — the raw count [XpBreakdown.newCards] already
 * multiplied into a total, needed back by the Summary screen to show that line's arithmetic.
 */
data class SessionXpResult(
    val breakdown: XpBreakdown,
    val newScoringState: ScoringState,
    val levelsCrossed: List<Int>,
    val newCardsStudied: Int,
)
