package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.SubcategoryProgress
import com.rossomak.flashcards.core.domain.repository.CardProgressRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Thin wrapper around [CardProgressRepository.getProgress], mirroring [GetFlashcardsUseCase]'s shape:
 * one Subcategory id in, its packed progress document out. Both Study Session ViewModels fan this out
 * once per Subcategory in scope at session start (ticket 04 of spec 04 session persistence) to learn
 * which cards are new and which were previously Mastered — never chunked or batched, since packing
 * already removed the old `whereIn` thirty-id cap this read used to be subject to.
 */
class GetSubcategoryProgressUseCase @Inject constructor(
    private val repository: CardProgressRepository,
) : UseCase<String, Result<SubcategoryProgress?>> {

    override suspend operator fun invoke(params: String): Result<SubcategoryProgress?> =
        repository.getProgress(subcategoryId = params)
}
