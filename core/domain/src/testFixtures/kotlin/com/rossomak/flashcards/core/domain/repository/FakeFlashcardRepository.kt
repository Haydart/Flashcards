package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.Subcategory

class FakeFlashcardRepository : FlashcardRepository {
    var categoriesToReturn: Result<List<Category>> = Result.success(emptyList())
    var subcategoriesToReturn: Result<List<Subcategory>> = Result.success(emptyList())
    var flashcardsToReturn: Result<List<Flashcard>> = Result.success(emptyList())
    val flashcardsBySubcategory: MutableMap<String, Result<List<Flashcard>>> = mutableMapOf()

    /** Prefix -> result, so a test can prove a repeated query was served from cache and never re-issued. */
    val searchResultsByPrefix: MutableMap<String, Result<List<Subcategory>>> = mutableMapOf()
    var searchResultsToReturn: Result<List<Subcategory>> = Result.success(emptyList())

    /** Every prefix [searchSubcategories] was called with, in call order. */
    val searchedPrefixes: MutableList<String> = mutableListOf()

    override suspend fun fetchCategories(): Result<List<Category>> = categoriesToReturn

    override suspend fun fetchSubcategories(categoryId: String): Result<List<Subcategory>> = subcategoriesToReturn

    override suspend fun searchSubcategories(namePrefix: String): Result<List<Subcategory>> {
        searchedPrefixes += namePrefix
        return searchResultsByPrefix[namePrefix] ?: searchResultsToReturn
    }

    override suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>> =
        flashcardsBySubcategory[subcategoryId] ?: flashcardsToReturn
}
