package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CurationRequest
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class GetCurationRequestsUseCase @Inject constructor(
    private val curationRepository: CurationRepository,
) : UseCase<List<String>, Result<Map<String, CurationRequest>>> {

    override suspend fun invoke(params: List<String>): Result<Map<String, CurationRequest>> =
        curationRepository.getCurationRequests(params)
}
