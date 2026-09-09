package com.rossomak.flashcards.core.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.ProgressSummaryDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressSummaryDto
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Reads the User's per-Subcategory progress-summary singleton, `users/{uid}/progress/summary`
 * (ADR-0016). Read-only: the server-authoritative `submitStudySession` Cloud Function (spec 08) is
 * the sole writer of this document now — this client never composes an increment for it.
 */
class ProgressSummaryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    suspend fun getSummary(): ProgressSummaryDto? {
        val document = firestore.collection(COLLECTION_PATH_TEMPLATE.format(uid)).document(DOCUMENT_ID).get().await()
        if (!document.exists()) return null

        @Suppress("UNCHECKED_CAST")
        val subcategoriesRaw = document.get(FIELD_SUBCATEGORIES) as? Map<String, Any> ?: emptyMap()
        val subcategories = subcategoriesRaw.mapNotNull { (subcategoryId, value) ->
            @Suppress("UNCHECKED_CAST")
            val entryMap = value as? Map<String, Any> ?: return@mapNotNull null
            subcategoryId to SubcategoryProgressSummaryDto(
                masteredCount = (entryMap[FIELD_MASTERED_COUNT] as? Number)?.toInt() ?: 0,
                studiedCount = (entryMap[FIELD_STUDIED_COUNT] as? Number)?.toInt() ?: 0,
            )
        }.toMap()

        return ProgressSummaryDto(subcategories = subcategories)
    }

    private companion object {
        const val COLLECTION_PATH_TEMPLATE = "users/%s/progress"
        const val DOCUMENT_ID = "summary"
        const val FIELD_SUBCATEGORIES = "subcategories"
        const val FIELD_MASTERED_COUNT = "masteredCount"
        const val FIELD_STUDIED_COUNT = "studiedCount"
    }
}
