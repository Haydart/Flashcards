package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.Subcategory

interface FlashcardRepository {

    suspend fun fetchCategories(): Result<List<Category>>

    suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>>

    /**
     * Subcategories across every category whose name starts with [namePrefix], ordered by name
     * ascending and capped to a small page — a live query, never a bulk load of the collection.
     * Matching is prefix-only and case-insensitive; [namePrefix] is normalized by the caller.
     */
    suspend fun searchSubcategories(namePrefix: String): Result<List<Subcategory>>

    suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>>
}
