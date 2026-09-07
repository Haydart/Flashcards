package com.rossomak.flashcards.core.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.ProgressSummaryDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressSummaryDto
import com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Reads and, via [toMergeFields], maps the write shape of the User's per-Subcategory progress-summary
 * singleton, `users/{uid}/state/progressSummary` (ADR-0016). Writing is not committed here — the
 * summary's increments must land in the same batch as the session document and every packed progress
 * write, so [documentReference] and [toMergeFields] are the seam
 * [StudySessionRemoteDataSource][com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource]
 * uses to fold this document into that batch, rather than committing its own.
 */
class ProgressSummaryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    fun documentReference(): DocumentReference =
        firestore.collection(COLLECTION_PATH_TEMPLATE.format(uid)).document(DOCUMENT_ID)

    suspend fun getSummary(): ProgressSummaryDto? {
        val document = documentReference().get().await()
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

    /**
     * Nested-key [FieldValue.increment]s only — never a wholesale rewrite of [FIELD_SUBCATEGORIES] —
     * so a Subcategory this write does not mention is left untouched, and an increment against a
     * missing field or a missing document creates it starting from zero (ADR-0016).
     */
    fun toMergeFields(write: ProgressSummaryWrite): Map<String, Any> = mapOf(
        FIELD_SUBCATEGORIES to write.subcategoryDeltas.mapValues { (_, delta) ->
            mapOf(
                FIELD_MASTERED_COUNT to FieldValue.increment(delta.masteredDelta.toLong()),
                FIELD_STUDIED_COUNT to FieldValue.increment(delta.studiedDelta.toLong()),
            )
        },
    )

    private companion object {
        const val COLLECTION_PATH_TEMPLATE = "users/%s/state"
        const val DOCUMENT_ID = "progressSummary"
        const val FIELD_SUBCATEGORIES = "subcategories"
        const val FIELD_MASTERED_COUNT = "masteredCount"
        const val FIELD_STUDIED_COUNT = "studiedCount"
    }
}
