package com.rossomak.flashcards.core.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.CategoryDto
import com.rossomak.flashcards.core.data.model.FlashcardDto
import com.rossomak.flashcards.core.data.model.SubcategoryDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FlashcardRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getCategories(): List<CategoryDto> =
        firestore.collection(COLLECTION_CATEGORIES)
            .orderBy(FIELD_ORDER)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(CategoryDto::class.java)?.copy(id = document.id)
            }

    suspend fun getSubcategoriesByCategoryId(categoryId: String): List<SubcategoryDto> =
        firestore.collection(COLLECTION_SUBCATEGORIES)
            .whereEqualTo(FIELD_CATEGORY_ID, categoryId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(SubcategoryDto::class.java)?.copy(id = document.id)
            }

    suspend fun getFlashcardsBySubcategoryId(subcategoryId: String): List<FlashcardDto> =
        firestore.collection(COLLECTION_SUBCATEGORIES)
            .document(subcategoryId)
            .collection(COLLECTION_FLASHCARDS)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(FlashcardDto::class.java)?.copy(id = document.id)
            }

    companion object {
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_SUBCATEGORIES = "subcategories"
        const val COLLECTION_FLASHCARDS = "flashcards"
        const val FIELD_ORDER = "order"
        const val FIELD_CATEGORY_ID = "categoryId"
    }
}
