package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

/**
 * Real Cloud Function client. Compiles and is fully wired, but only reachable once a deployed
 * function URL is configured via `VOICE_GRADING_BASE_URL` in `local.properties` — until then
 * [VoiceGradingApiRouter] routes every stage to [FakeVoiceGradingApi].
 */
class RetrofitVoiceGradingApi @Inject constructor(
    private val service: VoiceGradingRetrofitService,
) : VoiceGradingApi {

    override suspend fun gradeVoiceAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        wavBytes: ByteArray,
    ): VoiceAnswerGradeDto = service.gradeVoiceAnswer(
        audio = wavBytes.toAudioPart(),
        cardId = cardId.toTextPart(),
        question = question.toTextPart(),
        expectedAnswer = expectedAnswer.toTextPart(),
    )

    override suspend fun transcribe(wavBytes: ByteArray): TranscriptionDto =
        service.transcribe(wavBytes.toAudioPart())

    override suspend fun sanitizeAndGrade(request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto =
        service.sanitizeAndGrade(request)

    override suspend fun checkEntitlement(): EntitlementDto = service.checkEntitlement()

    private fun ByteArray.toAudioPart(): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            AUDIO_PART_NAME,
            AUDIO_FILE_NAME,
            toRequestBody(WAV_MEDIA_TYPE.toMediaType()),
        )

    private fun String.toTextPart(): RequestBody = toRequestBody(TEXT_MEDIA_TYPE.toMediaType())

    private companion object {
        const val AUDIO_PART_NAME = "audio"
        const val AUDIO_FILE_NAME = "answer.wav"
        const val WAV_MEDIA_TYPE = "audio/wav"
        const val TEXT_MEDIA_TYPE = "text/plain"
    }
}
