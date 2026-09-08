package com.rossomak.flashcards.core.data.source

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressSummaryDelta
import com.rossomak.flashcards.core.domain.model.XpBreakdown
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * Writes the `sessions/{sessionId}` document (ADR-0014), every [SessionCommit.progressWrites]
 * Subcategory progress document, and the [SessionCommit.progressSummaryWrite] increments (all
 * ADR-0016) — as one Firestore batch. Spec 05 ticket 02 adds the [SessionCommit.newScoringState]
 * write (`progress/user-stats`) to this same [FirebaseFirestore.batch], as a further line in
 * [commitSession] rather than a restructure, plus [SessionCommit.xpBreakdown] onto the session
 * document itself via [toDocumentFields].
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
    private val progressSummaryRemoteDataSource: ProgressSummaryRemoteDataSource,
    private val scoringStateRemoteDataSource: ScoringStateRemoteDataSource,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    fun commitSession(sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit) {
        val sessionResult = sessionCommit.sessionResult
        if (sessionResult is SessionResult.Fast) {
            commitFastSession(sessionResult, sessionCommit, onRejected)
        } else {
            commitBatchedSession(sessionResult, sessionCommit, onRejected)
        }
    }

    /**
     * Rated's commit path — one fire-and-forget batch, as before. A concurrent commit racing the
     * same card's `masteredCount` delta is a known, accepted drift (ADR-0016; the CR-69 #1 thread),
     * not fixed here: a transaction would need a live round trip, which conflicts with this batch
     * being deliberately un-awaited so an offline session can still commit (see [commitSession]'s
     * class doc).
     */
    private fun commitBatchedSession(sessionResult: SessionResult, sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit) {
        val sessionDocRef = firestore
            .collection(COLLECTION_PATH_TEMPLATE.format(uid))
            .document(sessionResult.id)

        val batch = firestore.batch()
        batch.set(sessionDocRef, sessionResult.toDocumentFields(sessionCommit.newCardsStudied, sessionCommit.xpBreakdown))
        sessionCommit.progressWrites.forEach { write ->
            val progressDocRef = cardProgressRemoteDataSource.documentReference(write.subcategoryId)
            batch.set(progressDocRef, cardProgressRemoteDataSource.toMergeFields(write), SetOptions.merge())
        }
        // A session whose deltas are all zero (e.g. every card defended) adds no line at all — the
        // whole point of ticket 03's per-subcategory filtering is that there is nothing to write.
        if (sessionCommit.progressSummaryWrite.subcategoryDeltas.isNotEmpty()) {
            val summaryDocRef = progressSummaryRemoteDataSource.documentReference()
            batch.set(summaryDocRef, progressSummaryRemoteDataSource.toMergeFields(sessionCommit.progressSummaryWrite), SetOptions.merge())
        }
        batch.set(scoringStateRemoteDataSource.documentReference(), scoringStateRemoteDataSource.toSetFields(sessionCommit.newScoringState))
        batch.commit().addOnFailureListener(DIRECT_EXECUTOR) { exception -> onRejected(exception) }
    }

    /**
     * Fast's commit path — a transaction, not a batch. [SessionCommit.progressWrites] was built by
     * [com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase] from a plain, possibly
     * stale, read of prior progress; Fast's create-if-absent rule (ADR-0016) only actually holds if
     * "absent" is re-checked against the server at write time. Without this, a concurrent Rated
     * commit landing `Mastered` between that stale read and this write gets clobbered back to `Seen`
     * (CR-69 #5) — a real state regression, unlike #1's self-healing count drift, so it is worth the
     * transaction's offline cost: a Fast session touching this path needs connectivity to commit.
     *
     * Every card this transaction still finds absent is re-derived here, from the transaction's own
     * fresh read — [SessionCommit.newCardsStudied] and its summary deltas are recomputed to match,
     * rather than trusting the outer, possibly-stale count. [SessionCommit.xpBreakdown]'s `newCards`
     * subtotal is **not** re-derived the same way: it was computed upstream from the outer count, so
     * under the same rare race this re-verification guards against, the persisted `newCardsStudied`
     * field can end up slightly ahead of the XP actually awarded for it. Accepted as the same class of
     * drift as CR-69 #1, not a new hazard this ticket introduces.
     */
    private fun commitFastSession(sessionResult: SessionResult.Fast, sessionCommit: SessionCommit, onRejected: (Throwable) -> Unit) {
        val sessionDocRef = firestore
            .collection(COLLECTION_PATH_TEMPLATE.format(uid))
            .document(sessionResult.id)

        firestore.runTransaction { transaction ->
            // Every transaction.get() must run before any transaction.set() — Firestore rejects a
            // transaction that interleaves them — so the fresh reads are all taken first, and only
            // then is it safe to decide and issue the writes.
            val stillAbsentCardsBySubcategory = sessionCommit.progressWrites.associate { write ->
                val progressDocRef = cardProgressRemoteDataSource.documentReference(write.subcategoryId)
                val existingCardIds = cardProgressRemoteDataSource.existingCardIds(transaction.get(progressDocRef))
                write.subcategoryId to write.cards.filterKeys { cardId -> cardId !in existingCardIds }
            }

            var newCardsStudied = 0
            val summaryDeltas = mutableMapOf<String, SubcategoryProgressSummaryDelta>()
            sessionCommit.progressWrites.forEach { write ->
                val stillAbsentCards = stillAbsentCardsBySubcategory.getValue(write.subcategoryId)
                if (stillAbsentCards.isNotEmpty()) {
                    val progressDocRef = cardProgressRemoteDataSource.documentReference(write.subcategoryId)
                    transaction.set(
                        progressDocRef,
                        cardProgressRemoteDataSource.toMergeFields(write.copy(cards = stillAbsentCards)),
                        SetOptions.merge(),
                    )
                    newCardsStudied += stillAbsentCards.size
                    summaryDeltas[write.subcategoryId] = SubcategoryProgressSummaryDelta(masteredDelta = 0, studiedDelta = stillAbsentCards.size)
                }
            }

            transaction.set(sessionDocRef, sessionResult.toDocumentFields(newCardsStudied, sessionCommit.xpBreakdown))
            if (summaryDeltas.isNotEmpty()) {
                val summaryDocRef = progressSummaryRemoteDataSource.documentReference()
                transaction.set(summaryDocRef, progressSummaryRemoteDataSource.toMergeFields(ProgressSummaryWrite(summaryDeltas)), SetOptions.merge())
            }
            transaction.set(scoringStateRemoteDataSource.documentReference(), scoringStateRemoteDataSource.toSetFields(sessionCommit.newScoringState))
            null
        }.addOnFailureListener(DIRECT_EXECUTOR) { exception -> onRejected(exception) }
    }

    /**
     * The document's own shape follows [SessionResult]'s sealed branch: [FIELD_CARDS_MASTERED]/
     * [FIELD_CARDS_PARTIAL]/[FIELD_CARDS_DEFENDED]/[FIELD_CARDS_DEMASTERED] are present only for a
     * [SessionResult.Rated] — genuinely absent from a Fast document, not zeroed. [FIELD_CARDS_DEFENDED]
     * and [FIELD_CARDS_DEMASTERED] are written as zero on every Rated document until spec 07 produces
     * a defended or de-mastered card. [newCardsStudied] comes from [CommitStudySessionUseCase][com.rossomak.flashcards.core.domain.usecase.CommitStudySessionUseCase]'s
     * read of prior progress — mode-agnostic, unlike the four counts above.
     *
     * [xpBreakdown]'s fields (spec 05 ticket 02), by contrast, are written for **both** modes, every
     * one always present: unlike the Rated-only counts above, an [XpBreakdown] field a Fast session
     * cannot earn is a genuine, always-true zero, not an undefined concept, so there is nothing to
     * omit.
     */
    private fun SessionResult.toDocumentFields(newCardsStudied: Int, xpBreakdown: XpBreakdown): Map<String, Any> = buildMap {
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
        put(FIELD_CARD_RESULTS, cardResults.associate { entry -> entry.cardId to entry.toResultFields() })
        if (this@toDocumentFields is SessionResult.Rated) {
            put(FIELD_CARDS_MASTERED, this@toDocumentFields.masteredCount)
            put(FIELD_CARDS_PARTIAL, this@toDocumentFields.partialCount)
            put(FIELD_CARDS_DEFENDED, 0) // spec 07 fills this in
            put(FIELD_CARDS_DEMASTERED, 0) // spec 07 fills this in
        }
        putAll(xpBreakdown.toFields())
    }

    private fun XpBreakdown.toFields(): Map<String, Any> = mapOf(
        FIELD_XP_NEW_CARDS to newCards,
        FIELD_XP_MASTERED to mastered,
        FIELD_XP_PARTIAL to partial,
        FIELD_XP_MASTERY_DEFENSE_BONUS to masteryDefenseBonus,
        FIELD_XP_DEMASTERED to demastered,
        FIELD_XP_TIME_STUDIED to timeStudied,
        FIELD_XP_SESSION_COMPLETION_BONUS to sessionCompletionBonus,
        FIELD_XP_DAILY_GOAL_BONUS to dailyGoalBonus,
        FIELD_XP_STREAK_BONUS to streakBonus,
        FIELD_XP_TOTAL to xpTotal,
    )

    private fun FlashcardResult.toResultFields(): Map<String, Any> = buildMap {
        put(FIELD_CARD_SUBCATEGORY_ID, subcategoryId)
        put(FIELD_STATE, state.name)
        if (this@toResultFields is FlashcardResult.Rated) {
            put(FIELD_ATTEMPTS_USED, this@toResultFields.attemptsUsed)
            put(FIELD_WAS_PREVIOUSLY_MASTERED, this@toResultFields.wasPreviouslyMastered)
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

        // spec 05 ticket 02's itemised XP breakdown, mirroring XpBreakdown's own field names 1:1.
        const val FIELD_XP_NEW_CARDS = "newCards"
        const val FIELD_XP_MASTERED = "mastered"
        const val FIELD_XP_PARTIAL = "partial"
        const val FIELD_XP_MASTERY_DEFENSE_BONUS = "masteryDefenseBonus"
        const val FIELD_XP_DEMASTERED = "demastered"
        const val FIELD_XP_TIME_STUDIED = "timeStudied"
        const val FIELD_XP_SESSION_COMPLETION_BONUS = "sessionCompletionBonus"
        const val FIELD_XP_DAILY_GOAL_BONUS = "dailyGoalBonus"
        const val FIELD_XP_STREAK_BONUS = "streakBonus"
        const val FIELD_XP_TOTAL = "xpTotal"

        const val FIELD_CARD_SUBCATEGORY_ID = "subcategoryId"
        const val FIELD_STATE = "state"
        const val FIELD_ATTEMPTS_USED = "attemptsUsed"
        const val FIELD_WAS_PREVIOUSLY_MASTERED = "wasPreviouslyMastered"
    }
}
