package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest

class FakeCurationRepository : CurationRepository {
    var curationRequestsToReturn: Result<Map<String, CurationRequest>> = Result.success(emptyMap())
    var upsertResultToReturn: Result<Unit> = Result.success(Unit)

    /** Every submission, in call order: card id, subcategory id, the whole reported set. */
    val submittedReports: MutableList<Triple<String, String, Set<CurationAction>>> = mutableListOf()

    override suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>> =
        curationRequestsToReturn

    override suspend fun upsertCurationActions(
        cardId: String,
        subcategoryId: String,
        actions: Set<CurationAction>,
    ): Result<Unit> {
        submittedReports.add(Triple(cardId, subcategoryId, actions))
        return upsertResultToReturn
    }
}
