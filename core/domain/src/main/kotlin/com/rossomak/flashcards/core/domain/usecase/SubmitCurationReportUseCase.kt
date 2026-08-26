package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.CurationAction
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Files one "Report a problem" submission: the whole checked set in a single write (ADR-0017).
 *
 * Additive — it never withdraws an action the user did not see in this dialog. The report draft
 * always starts empty, so an unchecked row means "not reported now", never "withdraw the report I
 * filed last week".
 */
class SubmitCurationReportUseCase @Inject constructor(
    private val curationRepository: CurationRepository,
) : UseCase<SubmitCurationReportUseCase.Params, Result<Unit>> {

    data class Params(
        val cardId: String,
        val subcategoryId: String,
        val actions: Set<CurationAction>,
    )

    override suspend fun invoke(params: Params): Result<Unit> = with(params) {
        if (actions.isEmpty()) {
            Result.success(Unit)
        } else {
            curationRepository.upsertCurationActions(cardId, subcategoryId, actions)
        }
    }
}
