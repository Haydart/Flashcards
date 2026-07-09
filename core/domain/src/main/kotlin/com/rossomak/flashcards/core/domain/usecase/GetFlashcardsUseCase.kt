package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class GetFlashcardsUseCase @Inject constructor(private val repository: FlashcardRepository) : UseCase<String, Result<List<Flashcard>>> {

    override suspend operator fun invoke(params: String): Result<List<Flashcard>> = repository.fetchFlashcards(subcategoryId = params)
}
