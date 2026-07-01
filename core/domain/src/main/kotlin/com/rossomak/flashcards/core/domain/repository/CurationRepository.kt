package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.model.CurationRequest

interface CurationRepository {
    suspend fun getCurationRequests(cardIds: List<String>): Result<Map<String, CurationRequest>>
    suspend fun upsertCurationAction(cardId: String, subcategoryId: String, action: CurationAction): Result<Unit>
    suspend fun removeCurationAction(cardId: String, action: CurationAction): Result<Unit>
}
