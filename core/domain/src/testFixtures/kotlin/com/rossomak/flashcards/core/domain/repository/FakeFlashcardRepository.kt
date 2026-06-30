package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.Subcategory

class FakeFlashcardRepository : FlashcardRepository {
    var categoriesToReturn: Result<List<Category>> = Result.success(emptyList())
    var subcategoriesToReturn: Result<List<Subcategory>> = Result.success(emptyList())
    var flashcardsToReturn: Result<List<Flashcard>> = Result.success(emptyList())

    override suspend fun fetchCategories(): Result<List<Category>> = categoriesToReturn

    override suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>> = subcategoriesToReturn

    override suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>> = flashcardsToReturn
}
