package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.BuildConfig
import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes each pipeline stage to the fake or the real Cloud Function client, per the debug
 * screen's stage toggles. A stage can only go real once `VOICE_GRADING_BASE_URL` is configured
 * in `local.properties`; otherwise the fake is used regardless of the toggle, so a missing
 * deployment can never crash the pipeline.
 */
@Singleton
class VoiceGradingApiRouter @Inject constructor(
    private val fakeApi: FakeVoiceGradingApi,
    private val retrofitApi: RetrofitVoiceGradingApi,
    private val debugSettings: VoicePipelineDebugSettings,
) : VoiceGradingApi {

    val isRealBackendConfigured: Boolean
        get() = BuildConfig.VOICE_GRADING_BASE_URL.isNotBlank()

    override suspend fun gradeVoiceAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        wavBytes: ByteArray,
    ): VoiceAnswerGradeDto = apiFor(debugSettings.toggles.value.useRealGrading)
        .gradeVoiceAnswer(cardId, question, expectedAnswer, wavBytes)

    override suspend fun transcribe(wavBytes: ByteArray): TranscriptionDto =
        apiFor(debugSettings.toggles.value.useRealTranscription).transcribe(wavBytes)

    override suspend fun sanitizeAndGrade(request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto =
        apiFor(debugSettings.toggles.value.useRealGrading).sanitizeAndGrade(request)

    override suspend fun checkEntitlement(): EntitlementDto =
        apiFor(debugSettings.toggles.value.useRealEntitlement).checkEntitlement()

    private fun apiFor(useReal: Boolean): VoiceGradingApi =
        if (useReal && isRealBackendConfigured) retrofitApi else fakeApi
}
