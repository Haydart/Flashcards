package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

class GetSubcategoriesUseCase @Inject constructor(private val repository: FlashcardRepository) :
    UseCase<String, Result<List<Subcategory>>> {

    override suspend operator fun invoke(params: String): Result<List<Subcategory>> = repository.fetchSubcategories(categoryId = params)
}
