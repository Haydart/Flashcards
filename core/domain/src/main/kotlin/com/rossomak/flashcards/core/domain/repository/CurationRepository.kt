package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest

interface CurationRepository {

    suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>>

    /**
     * Files a whole report submission in one write (ADR-0017). Additive: actions already stored on
     * the card and not named here are left alone, except a difficulty action's opposite, which is
     * cleared so a card is never simultaneously flagged too easy and too hard.
     */
    suspend fun upsertCurationActions(cardId: String, subcategoryId: String, actions: Set<CurationAction>): Result<Unit>

    /**
     * Withdraws one action, deleting the document when it was the last one. No screen calls this
     * today — the report dialog is additive — but it is the withdraw primitive ADR-0017 documents
     * and the shape the admin sync tooling mirrors.
     */
    suspend fun removeCurationAction(cardId: String, action: CurationAction): Result<Unit>
}
