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

    /**
     * A Subcategory's whole flashcard pool.
     *
     * Cached: repeat calls within a cache generation are served without contacting the backend, so
     * a caller may re-read freely rather than holding a pool of its own. Invalidated only by
     * [invalidateFlashcardCache].
     */
    suspend fun fetchFlashcards(subcategoryId: String): Result<List<Flashcard>>

    /**
     * Starts a new cache generation: the next [fetchFlashcards] for each Subcategory goes to the
     * backend, and reads after that are served from cache again.
     *
     * Nothing calls this yet. It is the seam the seed-versioned invalidation effort drives — that
     * effort compares a server-side knowledge-base seed against a locally stored copy at app start
     * and calls this on a mismatch (ADR-0038). Until it lands, a curation update stays invisible
     * on-device for the life of a cache entry.
     */
    fun invalidateFlashcardCache()
}
