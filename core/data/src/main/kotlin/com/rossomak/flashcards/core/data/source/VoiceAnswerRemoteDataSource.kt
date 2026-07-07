package com.rossomak.flashcards.core.data.source

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Persists graded voice answers to Firestore. Only the sanitized transcript, grade and
 * feedback are ever written — never audio, never an un-sanitized transcript (design doc's
 * data-retention guarantee).
 */
class VoiceAnswerRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) {

    private val uid: String
        get() = requireNotNull(firebaseAuth.currentUser?.uid) { "No authenticated user" }

    private fun collection() = firestore.collection("$USERS_COLLECTION/$uid/$VOICE_ANSWERS_COLLECTION")

    suspend fun saveVoiceAnswerGrade(cardId: String, grade: VoiceAnswerGrade) {
        val document = mapOf(
            FIELD_CARD_ID to cardId,
            FIELD_SANITIZED_TRANSCRIPT to grade.sanitizedTranscript,
            FIELD_GRADE_PERCENT to grade.gradePercent,
            FIELD_FEEDBACK to grade.feedback,
            FIELD_GRADED_AT to FieldValue.serverTimestamp(),
        )
        collection().add(document).await()
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val VOICE_ANSWERS_COLLECTION = "voiceAnswers"
        const val FIELD_CARD_ID = "cardId"
        const val FIELD_SANITIZED_TRANSCRIPT = "sanitizedTranscript"
        const val FIELD_GRADE_PERCENT = "gradePercent"
        const val FIELD_FEEDBACK = "feedback"
        const val FIELD_GRADED_AT = "gradedAt"
    }
}
