package com.rossomak.flashcards.core.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.CategoryDto
import com.rossomak.flashcards.core.data.model.FlashcardDto
import com.rossomak.flashcards.core.data.model.FlashcardShardDto
import com.rossomak.flashcards.core.data.model.SubcategoryDto
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class FlashcardRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getCategories(): List<CategoryDto> = firestore.collection(COLLECTION_CATEGORIES)
        .orderBy(FIELD_ORDER)
        .get()
        .await()
        .documents
        .mapNotNull { document ->
            document.toObject(CategoryDto::class.java)?.copy(id = document.id)
        }

    suspend fun getSubcategoriesByCategoryId(categoryId: String): List<SubcategoryDto> = firestore.collection(COLLECTION_SUBCATEGORIES)
        .whereEqualTo(FIELD_CATEGORY_ID, categoryId)
        .get()
        .await()
        .documents
        .mapNotNull { document ->
            document.toObject(SubcategoryDto::class.java)?.copy(id = document.id)
        }

    /**
     * Prefix search over the whole flat `subcategories` collection, never a bulk load. Firestore
     * has no substring query, so this is a range query on the denormalized lowercase [FIELD_NAME_LOWER]:
     * everything from [namePrefix] up to (but excluding) that prefix followed by [PREFIX_RANGE_SENTINEL],
     * a code point high enough that no realistic name sorts past it. [namePrefix] must already be
     * lowercased and trimmed by the caller, since the comparison itself is case-sensitive.
     *
     * Results come back ordered by `nameLower` ascending — Firestore forces the range field to be
     * the first `orderBy`, so no explicit sort and no composite index is involved.
     */
    suspend fun searchSubcategoriesByNamePrefix(namePrefix: String): List<SubcategoryDto> = firestore.collection(COLLECTION_SUBCATEGORIES)
        .whereGreaterThanOrEqualTo(FIELD_NAME_LOWER, namePrefix)
        .whereLessThan(FIELD_NAME_LOWER, namePrefix + PREFIX_RANGE_SENTINEL)
        .limit(SEARCH_RESULT_LIMIT)
        .get()
        .await()
        .documents
        .mapNotNull { document ->
            document.toObject(SubcategoryDto::class.java)?.copy(id = document.id)
        }

    /**
     * Cards for a Subcategory are packed into byte-budgeted `shards/{n}` docs rather than one
     * Firestore document per card (ADR-0037) — this single `.get()` reads however many shard
     * docs the Subcategory has (typically one), then flattens each shard's `flashcards` map
     * values into one list. Each [FlashcardDto] carries its own `id` field now (no longer copied
     * from a per-card document id, since a shard doc's id is just its shard index) — the map key
     * itself is what a future admin curation-fix tool would address, not read by this method.
     */
    suspend fun getFlashcardsBySubcategoryId(subcategoryId: String): List<FlashcardDto> = firestore.collection(COLLECTION_SUBCATEGORIES)
        .document(subcategoryId)
        .collection(COLLECTION_SHARDS)
        .get()
        .await()
        .documents
        .mapNotNull { document -> document.toObject(FlashcardShardDto::class.java) }
        .flatMap { it.flashcards.values }

    companion object {
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_SUBCATEGORIES = "subcategories"
        const val COLLECTION_SHARDS = "shards"
        const val FIELD_ORDER = "order"
        const val FIELD_CATEGORY_ID = "categoryId"
        const val FIELD_NAME_LOWER = "nameLower"

        /**
         * Upper bound of a prefix range query: a Private Use Area code point that sorts above
         * every character a Subcategory name realistically contains, so `[prefix, prefix + this)`
         * covers exactly the documents starting with `prefix`.
         */
        const val PREFIX_RANGE_SENTINEL = "\uF8FF"

        /**
         * Ceiling on a single search query. Must stay at or below 30: the matched ids are destined
         * for a Firestore `whereIn` progress lookup once card mastery ships, and `whereIn` caps at
         * 30 values. See docs/design/category-search.md.
         */
        const val SEARCH_RESULT_LIMIT = 20L
    }
}
