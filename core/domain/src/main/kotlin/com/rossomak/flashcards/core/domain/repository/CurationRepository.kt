package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest

interface CurationRepository {

    suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>>

    /**
     * Files a whole report submission in one write (ADR-0017). Additive: actions already stored on
     * the card and not named here are left alone, except a difficulty action's opposite, which is
     * cleared so a card is never simultaneously flagged too easy and too hard.
     *
     * No-ops without an extra write when [actions] is already a subset of what this card is known to
     * have flagged — the implementation checks this lazily, fetching the card's current state on its
     * first write and caching it for the rest of the process. A resubmission of the same set is
     * never meaningful, so it is dropped instead of re-flagging the server on every reopen of the
     * report dialog.
     */
    suspend fun upsertCurationActions(cardId: String, subcategoryId: String, actions: Set<CurationAction>): Result<Unit>
}
