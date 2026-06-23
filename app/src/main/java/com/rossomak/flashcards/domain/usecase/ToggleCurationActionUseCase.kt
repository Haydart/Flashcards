package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.model.CurationAction
import com.rossomak.flashcards.domain.repository.CurationRepository
import com.rossomak.flashcards.domain.usecase.base.UseCase
import javax.inject.Inject

class ToggleCurationActionUseCase @Inject constructor(
    private val curationRepository: CurationRepository,
) : UseCase<ToggleCurationActionUseCase.Params, Result<Unit>> {

    data class Params(
        val cardId: String,
        val subcategoryId: String,
        val action: CurationAction,
        val isCurrentlyActive: Boolean,
    )

    override suspend fun invoke(params: Params): Result<Unit> = with(params) {
        if (isCurrentlyActive) {
            curationRepository.removeCurationAction(cardId, action)
        } else {
            curationRepository.upsertCurationAction(cardId, subcategoryId, action)
        }
    }
}
