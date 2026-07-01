package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.base.NoParamUseCase
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: FlashcardRepository
) : NoParamUseCase<Result<List<Category>>> {

    override suspend operator fun invoke(): Result<List<Category>> = repository.fetchCategories()
}
