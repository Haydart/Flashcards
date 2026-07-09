package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.mapper.toDomain
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Never writes to Firestore per answer (ADR-0014): grades are handed back to the caller only.
 * Batch persistence at session end belongs to the not-yet-built Rating/Attempt/Terminal-State
 * pipeline (ADR-0016/ADR-0026).
 */
class DefaultVoiceAnswerGradingRepository @Inject constructor(
    private val voiceGradingApi: VoiceGradingApi,
) : VoiceAnswerGradingRepository {

    override suspend fun gradeSpokenAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        obfuscatedAnswerWav: ByteArray,
    ): Result<VoiceAnswerGrade> = withContext(Dispatchers.IO) {
        try {
            val grade = retryWithBackoff {
                voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, obfuscatedAnswerWav)
            }.toDomain()
            Result.success(grade)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun transcribe(obfuscatedAnswerWav: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(voiceGradingApi.transcribe(obfuscatedAnswerWav).transcript)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }

    override suspend fun sanitizeAndGrade(
        question: String,
        expectedAnswer: String,
        rawTranscript: String,
    ): Result<VoiceAnswerGrade> = withContext(Dispatchers.IO) {
        try {
            val request = SanitizeAndGradeRequestDto(
                question = question,
                expectedAnswer = expectedAnswer,
                transcript = rawTranscript,
            )
            Result.success(voiceGradingApi.sanitizeAndGrade(request).toDomain())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    override suspend fun checkEntitlement(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Result.success(voiceGradingApi.checkEntitlement().isPremium)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /**
     * Exponential backoff on transient (IO) failures only — entitlement rejections and other
     * errors surface immediately. Design doc: upload failures retry, then fail audibly.
     */
    private suspend fun <T> retryWithBackoff(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (exception: IOException) {
                attempt++
                if (attempt >= MAX_UPLOAD_ATTEMPTS) throw exception
                delay(BASE_RETRY_DELAY_MS * (1L shl (attempt - 1)))
            }
        }
    }

    private companion object {
        const val MAX_UPLOAD_ATTEMPTS = 3
        const val BASE_RETRY_DELAY_MS = 500L
    }
}
