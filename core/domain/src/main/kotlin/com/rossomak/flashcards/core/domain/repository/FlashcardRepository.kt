package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.Subcategory

interface FlashcardRepository {

    suspend fun fetchCategories(): Result<List<Category>>

    suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>>

    suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>>
}
