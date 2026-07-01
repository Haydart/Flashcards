package com.rossomak.flashcards.core.data.source

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rossomak.flashcards.core.data.model.CurationActionEntryDto
import com.rossomak.flashcards.core.data.model.CurationRequestDto
import com.rossomak.flashcards.core.domain.model.CurationAction
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val WHEREIN_BATCH_SIZE = 30

class CurationRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    private fun collection() = firestore.collection("users/$uid/curationRequests")

    suspend fun getCurationRequests(cardIds: List<String>): Map<String, CurationRequestDto> {
        if (cardIds.isEmpty()) return emptyMap()
        return cardIds
            .chunked(WHEREIN_BATCH_SIZE)
            .flatMap { chunk ->
                collection()
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val subcategoryId = document.getString("subcategoryId") ?: return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val actionsRaw = document.get("actions") as? Map<String, Any> ?: emptyMap()
                        val actions = actionsRaw.mapNotNull { (key, value) ->
                            @Suppress("UNCHECKED_CAST")
                            val entryMap = value as? Map<String, Any> ?: return@mapNotNull null
                            val flaggedAt = entryMap["flaggedAt"] as? Timestamp
                            key to CurationActionEntryDto(flaggedAt = flaggedAt)
                        }.toMap()
                        document.id to CurationRequestDto(subcategoryId = subcategoryId, actions = actions)
                    }
            }
            .toMap()
    }

    suspend fun upsertCurationAction(cardId: String, subcategoryId: String, action: CurationAction) {
        val docRef = collection().document(cardId)
        val updates = mutableMapOf<String, Any>(
            "subcategoryId" to subcategoryId,
            "actions.${action.name}" to mapOf("flaggedAt" to FieldValue.serverTimestamp()),
        )
        action.difficultyOpposite()?.let { opposite ->
            updates["actions.${opposite.name}"] = FieldValue.delete()
        }
        docRef.set(updates, SetOptions.merge()).await()
    }

    suspend fun removeCurationAction(cardId: String, action: CurationAction) {
        val docRef = collection().document(cardId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) return@runTransaction
            @Suppress("UNCHECKED_CAST")
            val actionsMap = snapshot.get("actions") as? Map<String, Any> ?: emptyMap()
            if (!actionsMap.containsKey(action.name)) return@runTransaction
            if (actionsMap.size <= 1) {
                transaction.delete(docRef)
            } else {
                transaction.update(docRef, "actions.${action.name}", FieldValue.delete())
            }
        }.await()
    }
}
