package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest

class FakeCurationRepository : CurationRepository {
    var curationRequestsToReturn: Result<Map<String, CurationRequest>> = Result.success(emptyMap())
    val upsertedActions: MutableList<Triple<String, String, CurationAction>> = mutableListOf()
    val removedActions: MutableList<Pair<String, CurationAction>> = mutableListOf()

    override suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>> =
        curationRequestsToReturn

    override suspend fun upsertCurationAction(
        cardId: String,
        subcategoryId: String,
        action: CurationAction
    ): Result<Unit> {
        upsertedActions.add(Triple(cardId, subcategoryId, action))
        return Result.success(Unit)
    }

    override suspend fun removeCurationAction(cardId: String, action: CurationAction): Result<Unit> {
        removedActions.add(Pair(cardId, action))
        return Result.success(Unit)
    }
}
