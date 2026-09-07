package com.rossomak.flashcards.core.data.source

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * Writes the `sessions/{sessionId}` document (ADR-0014) and, since ticket 02, every
 * [SessionCommit.progressWrites] Subcategory progress document (ADR-0016) — as one Firestore batch.
 * Ticket 03 and spec 05 add the progress-summary increments and a further scoring-state write to
 * this same [FirebaseFirestore.batch], as further lines in [commitSession] rather than a restructure.
 *
 * **Not awaited on the success path.** Firestore's on-device persistence queues a batch locally and
 * only resolves [com.google.android.gms.tasks.Task] once connectivity returns and the backend
 * acknowledges it, so awaiting it here would hang indefinitely while offline. [commitSession]
 * therefore only *starts* the commit and reports success as soon as it has been handed to
 * Firestore; [onRejected] is attached to the same [com.google.android.gms.tasks.Task] and fires
 * later, asynchronously, only on a genuine rejection — never for a write that is merely queued.
 *
 * [onRejected] is attached with [DIRECT_EXECUTOR] rather than the default (main-thread) executor:
 * the callback only forwards to a thread-safe [kotlinx.coroutines.flow.MutableSharedFlow.tryEmit],
 * so there is nothing that needs the main thread, and the default executor requires an Android
 * `Looper` this plain-JVM `core:data` unit tests don't provide.
 */
class StudySessionRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cardProgressRemoteDataSource: CardProgressRemoteDataSource,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    fun commitSession(sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit) {
        val sessionResult = sessionCommit.sessionResult
        val sessionDocRef = firestore
            .collection(COLLECTION_PATH_TEMPLATE.format(uid))
            .document(sessionResult.id)

        val batch = firestore.batch()
        batch.set(sessionDocRef, sessionResult.toDocumentFields(sessionCommit.newCardsStudied))
        sessionCommit.progressWrites.forEach { write ->
            val progressDocRef = cardProgressRemoteDataSource.documentReference(write.subcategoryId)
            batch.set(progressDocRef, cardProgressRemoteDataSource.toMergeFields(write), SetOptions.merge())
        }
        batch.commit().addOnFailureListener(DIRECT_EXECUTOR) { exception -> onRejected(exception) }
    }

    /**
     * The document's own shape follows [SessionResult.mode]: [FIELD_CARDS_MASTERED]/
     * [FIELD_CARDS_PARTIAL]/[FIELD_CARDS_DEFENDED]/[FIELD_CARDS_DEMASTERED] are present only for a
     * Rated session — genuinely absent from a Fast document, not zeroed. [FIELD_CARDS_DEFENDED] and
     * [FIELD_CARDS_DEMASTERED] are written as zero on every Rated document until spec 07 produces a
     * defended or de-mastered card. [newCardsStudied] comes from [CommitStudySessionUseCase][com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase]'s
     * read of prior progress — mode-agnostic, unlike the four counts above.
     */
    private fun SessionResult.toDocumentFields(newCardsStudied: Int): Map<String, Any> = buildMap {
        put(FIELD_SESSION_ID, id)
        put(FIELD_START_TIMESTAMP, Timestamp(startedAt.epochSecond, startedAt.nano))
        put(FIELD_DURATION_SECONDS, durationSeconds)
        put(FIELD_STUDY_MODE, mode.name)
        put(FIELD_IS_ABANDONED, abandoned)
        put(FIELD_CATEGORY_ID, categoryId)
        put(FIELD_CATEGORY_NAME, categoryName)
        put(FIELD_SUBCATEGORY_IDS, subcategoryIds)
        put(FIELD_SUBCATEGORY_NAMES, subcategoryNames)
        put(FIELD_CARD_COUNT, studiedCount)
        put(FIELD_NEW_CARDS_STUDIED, newCardsStudied)
        put(FIELD_CARD_RESULTS, cardResults.associate { entry -> entry.cardId to entry.toResultFields(mode) })
        if (mode == StudyMode.Rated) {
            put(FIELD_CARDS_MASTERED, masteredCount)
            put(FIELD_CARDS_PARTIAL, partialCount)
            put(FIELD_CARDS_DEFENDED, 0) // spec 07 fills this in
            put(FIELD_CARDS_DEMASTERED, 0) // spec 07 fills this in
        }
    }

    private fun FlashcardResult.toResultFields(mode: StudyMode): Map<String, Any> = buildMap {
        put(FIELD_CARD_SUBCATEGORY_ID, subcategoryId)
        put(FIELD_STATE, state.name)
        if (mode == StudyMode.Rated) {
            put(FIELD_ATTEMPTS_USED, attemptsUsed)
            put(FIELD_WAS_PREVIOUSLY_MASTERED, wasPreviouslyMastered)
        }
    }

    private companion object {
        val DIRECT_EXECUTOR = Executor { runnable -> runnable.run() }

        const val COLLECTION_PATH_TEMPLATE = "users/%s/sessions"

        const val FIELD_SESSION_ID = "sessionId"
        const val FIELD_START_TIMESTAMP = "startTimestamp"
        const val FIELD_DURATION_SECONDS = "durationSeconds"
        const val FIELD_STUDY_MODE = "studyMode"
        const val FIELD_IS_ABANDONED = "isAbandoned"
        const val FIELD_CATEGORY_ID = "categoryId"
        const val FIELD_CATEGORY_NAME = "categoryName"
        const val FIELD_SUBCATEGORY_IDS = "subcategoryIds"
        const val FIELD_SUBCATEGORY_NAMES = "subcategoryNames"
        const val FIELD_CARD_COUNT = "cardCount"
        const val FIELD_NEW_CARDS_STUDIED = "newCardsStudied"
        const val FIELD_CARD_RESULTS = "cardResults"
        const val FIELD_CARDS_MASTERED = "cardsMastered"
        const val FIELD_CARDS_PARTIAL = "cardsPartial"
        const val FIELD_CARDS_DEFENDED = "cardsDefended"
        const val FIELD_CARDS_DEMASTERED = "cardsDemastered"

        const val FIELD_CARD_SUBCATEGORY_ID = "subcategoryId"
        const val FIELD_STATE = "state"
        const val FIELD_ATTEMPTS_USED = "attemptsUsed"
        const val FIELD_WAS_PREVIOUSLY_MASTERED = "wasPreviouslyMastered"
    }
}
