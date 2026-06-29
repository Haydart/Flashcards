package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.model.CurationRequest
import com.rossomak.flashcards.domain.repository.CurationRepository
import com.rossomak.flashcards.domain.usecase.base.UseCase
import javax.inject.Inject

class GetCurationRequestsUseCase @Inject constructor(
    private val curationRepository: CurationRepository,
) : UseCase<List<String>, Result<Map<String, CurationRequest>>> {

    override suspend fun invoke(params: List<String>): Result<Map<String, CurationRequest>> =
        curationRepository.getCurationRequests(params)
}
