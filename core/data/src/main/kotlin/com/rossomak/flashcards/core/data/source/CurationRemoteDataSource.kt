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
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private const val WHEREIN_BATCH_SIZE = 30

class CurationRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    private fun collection() = firestore.collection(COLLECTION_PATH_TEMPLATE.format(uid))

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
                        val subcategoryId = document.getString(FIELD_SUBCATEGORY_ID) ?: return@mapNotNull null

                        @Suppress("UNCHECKED_CAST")
                        val actionsRaw = document.get(FIELD_ACTIONS) as? Map<String, Any> ?: emptyMap()
                        val actions = actionsRaw.mapNotNull { (key, value) ->
                            @Suppress("UNCHECKED_CAST")
                            val entryMap = value as? Map<String, Any> ?: return@mapNotNull null
                            val flaggedAt = entryMap[FIELD_FLAGGED_AT] as? Timestamp
                            key to CurationActionEntryDto(flaggedAt = flaggedAt)
                        }.toMap()
                        document.id to CurationRequestDto(subcategoryId = subcategoryId, actions = actions)
                    }
            }
            .toMap()
    }

    /**
     * One `set(merge)` for the whole submission. Dotted field paths address individual map keys,
     * so untouched actions on the document survive; a difficulty action additionally deletes its
     * opposite, keeping the pair mutually exclusive in storage as well as in the draft.
     */
    suspend fun upsertCurationActions(cardId: String, subcategoryId: String, actions: Set<CurationAction>) {
        if (actions.isEmpty()) return
        val updates = mutableMapOf<String, Any>(FIELD_SUBCATEGORY_ID to subcategoryId)
        actions.forEach { action ->
            updates[actionFieldPath(action)] = mapOf(FIELD_FLAGGED_AT to FieldValue.serverTimestamp())
        }
        actions.forEach { action ->
            val opposite = action.difficultyOpposite() ?: return@forEach
            if (opposite !in actions) updates[actionFieldPath(opposite)] = FieldValue.delete()
        }
        collection().document(cardId).set(updates, SetOptions.merge()).await()
    }

    suspend fun removeCurationAction(cardId: String, action: CurationAction) {
        val docRef = collection().document(cardId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) return@runTransaction
            @Suppress("UNCHECKED_CAST")
            val actionsMap = snapshot.get(FIELD_ACTIONS) as? Map<String, Any> ?: emptyMap()
            if (!actionsMap.containsKey(action.name)) return@runTransaction
            if (actionsMap.size <= 1) {
                transaction.delete(docRef)
            } else {
                transaction.update(docRef, actionFieldPath(action), FieldValue.delete())
            }
        }.await()
    }

    private fun actionFieldPath(action: CurationAction): String = "$FIELD_ACTIONS.${action.name}"

    private companion object {
        const val COLLECTION_PATH_TEMPLATE = "users/%s/curationRequests"
        const val FIELD_SUBCATEGORY_ID = "subcategoryId"
        const val FIELD_ACTIONS = "actions"
        const val FIELD_FLAGGED_AT = "flaggedAt"
    }
}
