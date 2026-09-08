package com.rossomak.flashcards.core.data.source

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.CardProgressEntryDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressDto
import com.rossomak.flashcards.core.domain.model.CardProgressUpdate
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Reads and, via [toMergeFields], maps the write shape of
 * `users/{uid}/progress/details/subcategories/{subcategoryId}` (ADR-0016). `details` is a fixed
 * anchor document with no fields of its own — it exists only to host the real `subcategories`
 * subcollection, since Firestore cannot nest a collection directly inside another collection; the
 * sibling singletons `progress/summary` and `progress/user-stats` stay one hop shallower so
 * `progress` itself holds only per-User singleton documents. Writing is not committed here — a
 * session commit's progress writes must land in the
 * same batch as its session document, so [documentReference] and [toMergeFields] are the seam
 * [StudySessionRemoteDataSource][com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource]
 * uses to fold this collection's documents into that batch, rather than committing its own.
 */
class CardProgressRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    private fun collection() = firestore
        .collection(PROGRESS_COLLECTION_PATH_TEMPLATE.format(uid))
        .document(DETAILS_DOCUMENT_ID)
        .collection(SUBCATEGORIES_COLLECTION_ID)

    fun documentReference(subcategoryId: String): DocumentReference = collection().document(subcategoryId)

    suspend fun getProgress(subcategoryId: String): SubcategoryProgressDto? {
        val document = collection().document(subcategoryId).get().await()
        if (!document.exists()) return null

        val categoryId = document.getString(FIELD_CATEGORY_ID) ?: return null

        @Suppress("UNCHECKED_CAST")
        val cardsRaw = document.get(FIELD_CARDS) as? Map<String, Any> ?: emptyMap()
        val cards = cardsRaw.mapNotNull { (cardId, value) ->
            @Suppress("UNCHECKED_CAST")
            val entryMap = value as? Map<String, Any> ?: return@mapNotNull null
            cardId to CardProgressEntryDto(
                state = entryMap[FIELD_STATE] as? String ?: "",
                firstStudiedAt = entryMap[FIELD_FIRST_STUDIED_AT] as? Timestamp,
                masteredAt = entryMap[FIELD_MASTERED_AT] as? Timestamp,
            )
        }.toMap()

        return SubcategoryProgressDto(categoryId = categoryId, cards = cards)
    }

    /**
     * The nested-map shape a `set(merge)` writes back onto what [getProgress] reads. Only
     * [SubcategoryProgressWrite.cards] appears here — the whole point of a merge write is that
     * every card this session did not touch is left alone (ADR-0016), so nothing wholesale is ever
     * built.
     */
    fun toMergeFields(write: SubcategoryProgressWrite): Map<String, Any> = mapOf(
        FIELD_CATEGORY_ID to write.categoryId,
        FIELD_CARDS to write.cards.mapValues { (_, update) -> update.toEntryFields() },
    )

    private fun CardProgressUpdate.toEntryFields(): Map<String, Any> = buildMap {
        put(FIELD_STATE, state.name)
        if (stampFirstStudied) put(FIELD_FIRST_STUDIED_AT, FieldValue.serverTimestamp())
        if (stampMastered) put(FIELD_MASTERED_AT, FieldValue.serverTimestamp())
    }

    private companion object {
        const val PROGRESS_COLLECTION_PATH_TEMPLATE = "users/%s/progress"
        const val DETAILS_DOCUMENT_ID = "details"
        const val SUBCATEGORIES_COLLECTION_ID = "subcategories"
        const val FIELD_CATEGORY_ID = "categoryId"
        const val FIELD_CARDS = "cards"
        const val FIELD_STATE = "state"
        const val FIELD_FIRST_STUDIED_AT = "firstStudiedAt"
        const val FIELD_MASTERED_AT = "masteredAt"
    }
}
