package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.mapper.toDomain
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.VoiceGradingStreamEventDto
import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGradingEvent
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Never writes to Firestore per answer (ADR-0014): grades are handed back to the caller only.
 * Batch persistence at session end belongs to the not-yet-built Rating/Attempt/Terminal-State
 * pipeline (ADR-0016/ADR-0026).
 */
class DefaultVoiceAnswerGradingRepository @Inject constructor(
    private val voiceGradingApi: VoiceGradingApi,
) : VoiceAnswerGradingRepository {

    /**
     * Collects the single streamed call (ADR-0028), re-emitting each wire event as a domain
     * [VoiceAnswerGradingEvent]. A transient [IOException] retries the *whole* call from
     * scratch with backoff — the server has no partial-progress concept to resume, so a retry
     * after the transcript chunk already arrived simply re-emits a fresh
     * [VoiceAnswerGradingEvent.TranscriptReady] to the collector. Non-IO failures (including
     * entitlement rejections) propagate immediately as a flow exception, per this project's
     * Flow error convention — callers collect with `.catch()`.
     */
    override fun gradeSpokenAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        obfuscatedAnswerWav: ByteArray,
    ): Flow<VoiceAnswerGradingEvent> = flow {
        var attempt = 0
        while (true) {
            var sanitizedTranscript: String? = null
            try {
                voiceGradingApi.gradeVoiceAnswer(cardId, question, expectedAnswer, obfuscatedAnswerWav)
                    .collect { event ->
                        when (event) {
                            is VoiceGradingStreamEventDto.TranscriptChunk -> {
                                sanitizedTranscript = event.sanitizedTranscript
                                emit(VoiceAnswerGradingEvent.TranscriptReady(event.sanitizedTranscript))
                            }
                            is VoiceGradingStreamEventDto.Graded -> {
                                val grade = VoiceAnswerGrade(
                                    sanitizedTranscript = sanitizedTranscript.orEmpty(),
                                    gradePercent = event.gradePercent,
                                    feedback = event.feedback,
                                )
                                emit(VoiceAnswerGradingEvent.Graded(grade))
                            }
                        }
                    }
                return@flow
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                attempt++
                if (attempt >= MAX_UPLOAD_ATTEMPTS) throw exception
                delay(BASE_RETRY_DELAY_MS * (1L shl (attempt - 1)))
            }
        }
    }.flowOn(Dispatchers.IO)

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

    private companion object {
        const val MAX_UPLOAD_ATTEMPTS = 3
        const val BASE_RETRY_DELAY_MS = 500L
    }
}
