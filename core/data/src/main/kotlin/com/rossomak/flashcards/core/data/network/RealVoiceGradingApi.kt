package com.rossomak.flashcards.core.data.network

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.StreamResponse
import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import com.rossomak.flashcards.core.data.model.VoiceGradingStreamEventDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow

/**
 * Real Cloud Function client. Compiles and is fully wired, but only reachable once a deployed
 * function project is configured (`google-services.json` pointing at a real project with
 * `gradeVoiceAnswer` deployed) — until then [VoiceGradingApiRouter] routes every stage to
 * [FakeVoiceGradingApi]. `gradeVoiceAnswer` streams over a Firebase Callable Function
 * connection (ADR-0028); the remaining debug-only stages stay plain REST via
 * [VoiceGradingRetrofitService], since they don't need streaming (open decision 7, resolved
 * here: no reason to migrate endpoints that only ever return one response).
 */
class RealVoiceGradingApi @Inject constructor(
    private val service: VoiceGradingRetrofitService,
    private val functions: FirebaseFunctions,
) : VoiceGradingApi {

    override fun gradeVoiceAnswer(
        cardId: String,
        question: String,
        expectedAnswer: String,
        wavBytes: ByteArray,
    ): Flow<VoiceGradingStreamEventDto> {
        val payload = mapOf(
            FIELD_CARD_ID to cardId,
            FIELD_QUESTION to question,
            FIELD_EXPECTED_ANSWER to expectedAnswer,
            FIELD_AUDIO_BASE64 to Base64.encodeToString(wavBytes, Base64.NO_WRAP),
        )
        return functions.getHttpsCallable(GRADE_VOICE_ANSWER_FUNCTION_NAME)
            .stream(payload)
            .asFlow()
            .map { response -> response.toStreamEventDto() }
            .catch { throwable ->
                if (throwable is FirebaseFunctionsException &&
                    throwable.code == FirebaseFunctionsException.Code.PERMISSION_DENIED
                ) {
                    throw VoiceGradingEntitlementException()
                }
                throw throwable
            }
    }

    private fun StreamResponse.toStreamEventDto(): VoiceGradingStreamEventDto = when (this) {
        is StreamResponse.Message -> {
            val data = message.data as? Map<*, *> ?: error("Unexpected gradeVoiceAnswer chunk shape: ${message.data}")
            VoiceGradingStreamEventDto.TranscriptChunk(
                sanitizedTranscript = data[FIELD_SANITIZED_TRANSCRIPT] as? String ?: "",
            )
        }
        is StreamResponse.Result -> {
            val data = result.data as? Map<*, *> ?: error("Unexpected gradeVoiceAnswer result shape: ${result.data}")
            VoiceGradingStreamEventDto.Graded(
                gradePercent = (data[FIELD_GRADE] as? Number)?.toInt() ?: 0,
                feedback = data[FIELD_FEEDBACK] as? String ?: "",
            )
        }
        // StreamResponse isn't a Kotlin `sealed class` at the bytecode level (just `abstract`),
        // so the compiler can't prove Message/Result are exhaustive across module boundaries.
        else -> error("Unknown StreamResponse subtype: $this")
    }

    override suspend fun transcribe(wavBytes: ByteArray): TranscriptionDto =
        mappingEntitlementRejection { service.transcribe(wavBytes.toAudioPart()) }

    override suspend fun sanitizeAndGrade(request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto =
        mappingEntitlementRejection { service.sanitizeAndGrade(request) }

    override suspend fun checkEntitlement(): EntitlementDto =
        mappingEntitlementRejection { service.checkEntitlement() }

    /** Mirrors [FakeVoiceGradingApi]'s contract: a server-side 403 surfaces as [VoiceGradingEntitlementException]. */
    private inline fun <T> mappingEntitlementRejection(block: () -> T): T = try {
        block()
    } catch (exception: HttpException) {
        if (exception.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            throw VoiceGradingEntitlementException()
        }
        throw exception
    }

    private fun ByteArray.toAudioPart(): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            AUDIO_PART_NAME,
            AUDIO_FILE_NAME,
            toRequestBody(WAV_MEDIA_TYPE.toMediaType()),
        )

    private companion object {
        const val GRADE_VOICE_ANSWER_FUNCTION_NAME = "gradeVoiceAnswer"
        const val FIELD_CARD_ID = "card_id"
        const val FIELD_QUESTION = "question"
        const val FIELD_EXPECTED_ANSWER = "expected_answer"
        const val FIELD_AUDIO_BASE64 = "audio_base64"
        const val FIELD_SANITIZED_TRANSCRIPT = "sanitized_transcript"
        const val FIELD_GRADE = "grade"
        const val FIELD_FEEDBACK = "feedback"
        const val AUDIO_PART_NAME = "audio"
        const val AUDIO_FILE_NAME = "answer.wav"
        const val WAV_MEDIA_TYPE = "audio/wav"
    }
}
