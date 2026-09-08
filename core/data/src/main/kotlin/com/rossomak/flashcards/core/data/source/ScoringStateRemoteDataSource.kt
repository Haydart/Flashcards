package com.rossomak.flashcards.core.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.data.model.ScoringStateDto
import com.rossomak.flashcards.core.domain.model.ScoringState
import com.rossomak.flashcards.core.domain.model.ScoringState.Companion.STARTING_LEVEL
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/**
 * Reads and, via [toSetFields], maps the write shape of the User's account-wide scoring-state
 * singleton, `users/{uid}/progress/user-stats` (spec 05 ticket 02). Writing is not committed
 * here — the new state must land in the same batch as the session document and every other write, so
 * [documentReference] and [toSetFields] are the seam
 * [StudySessionRemoteDataSource][com.rossomak.flashcards.core.data.source.StudySessionRemoteDataSource]
 * uses to fold this document into that batch, rather than committing its own.
 *
 * Unlike [ProgressSummaryRemoteDataSource.toMergeFields]'s nested-key increments,
 * [toSetFields] is a **wholesale overwrite**: [CalculateSessionXpUseCase][com.rossomak.flashcards.core.domain.usecase.CalculateSessionXpUseCase]
 * already produces the complete next [ScoringState] from the complete prior one, so there is nothing
 * partial to merge — every field is rewritten every commit.
 */
class ScoringStateRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    fun documentReference(): DocumentReference =
        firestore.collection(COLLECTION_PATH_TEMPLATE.format(uid)).document(DOCUMENT_ID)

    suspend fun getScoringState(): ScoringStateDto? {
        val document = documentReference().get().await()
        if (!document.exists()) return null

        return ScoringStateDto(
            xp = document.getLong(FIELD_XP) ?: 0,
            level = (document.getLong(FIELD_LEVEL) ?: STARTING_LEVEL.toLong()).toInt(),
            xpIntoCurrentLevel = document.getLong(FIELD_XP_INTO_CURRENT_LEVEL) ?: 0,
            currentStreak = (document.getLong(FIELD_CURRENT_STREAK) ?: 0).toInt(),
            bestStreak = (document.getLong(FIELD_BEST_STREAK) ?: 0).toInt(),
            lastStudyDate = document.getString(FIELD_LAST_STUDY_DATE) ?: "",
            goalMetDate = document.getString(FIELD_GOAL_MET_DATE) ?: "",
        )
    }

    fun toSetFields(newState: ScoringState): Map<String, Any> = mapOf(
        FIELD_XP to newState.xp,
        FIELD_LEVEL to newState.level,
        FIELD_XP_INTO_CURRENT_LEVEL to newState.xpIntoCurrentLevel,
        FIELD_CURRENT_STREAK to newState.currentStreak,
        FIELD_BEST_STREAK to newState.bestStreak,
        FIELD_LAST_STUDY_DATE to newState.lastStudyDate,
        FIELD_GOAL_MET_DATE to newState.goalMetDate,
    )

    private companion object {
        const val COLLECTION_PATH_TEMPLATE = "users/%s/progress"
        const val DOCUMENT_ID = "user-stats"
        const val FIELD_XP = "xp"
        const val FIELD_LEVEL = "level"
        const val FIELD_XP_INTO_CURRENT_LEVEL = "xpIntoCurrentLevel"
        const val FIELD_CURRENT_STREAK = "currentStreak"
        const val FIELD_BEST_STREAK = "bestStreak"
        const val FIELD_LAST_STUDY_DATE = "lastStudyDate"
        const val FIELD_GOAL_MET_DATE = "goalMetDate"
    }
}
