package com.rossomak.flashcards.data.source

import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.data.model.CategoryDto
import com.rossomak.flashcards.data.model.FlashcardDto
import com.rossomak.flashcards.data.model.SubcategoryDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FlashcardRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getCategories(): List<CategoryDto> =
        firestore.collection("categories")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(CategoryDto::class.java)?.copy(id = document.id)
            }

    suspend fun getSubcategoriesByCategoryId(categoryId: String): List<SubcategoryDto> =
        firestore.collection("subcategories")
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(SubcategoryDto::class.java)?.copy(id = document.id)
            }

    suspend fun getFlashcardsBySubcategoryId(subcategoryId: String): List<FlashcardDto> =
        firestore.collection("subcategories")
            .document(subcategoryId)
            .collection("flashcards")
            .get()
            .await()
            .documents
            .mapNotNull { document ->
                document.toObject(FlashcardDto::class.java)?.copy(id = document.id)
            }
}
