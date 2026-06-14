package com.rossomak.flashcards.domain.usecase

import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.repository.FlashcardRepository
import com.rossomak.flashcards.domain.usecase.base.UseCase
import javax.inject.Inject

class GetFlashcardsUseCase @Inject constructor(
    private val repository: FlashcardRepository
) : UseCase<String, Result<List<Flashcard>>> {

    override suspend operator fun invoke(params: String): Result<List<Flashcard>> =
        repository.fetchFlashcards(subcategoryId = params)
}
