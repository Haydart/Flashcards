package com.rossomak.flashcards.core.domain.usecase

import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Debug: transcribe + sanitize an obfuscated clip without grading (ADR-0029 §4). Collapses the old
 * `TranscribeVoiceClipUseCase` + `SanitizeAndGradeTranscriptUseCase` pair — the backend now serves
 * this via the payload-inferred debug mode of the single grade callable.
 */
class TranscribeAndSanitizeUseCase @Inject constructor(
    private val voiceAnswerGradingRepository: VoiceAnswerGradingRepository,
) : UseCase<ByteArray, Result<String>> {

    override suspend fun invoke(params: ByteArray): Result<String> =
        voiceAnswerGradingRepository.transcribeAndSanitize(params)
}
