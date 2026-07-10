package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit binding for the Cloud Function proxy's plain-REST endpoints (debug-only
 * transcribe/sanitizeAndGrade, plus entitlement). `gradeVoiceAnswer` migrated to a Firebase
 * Callable streaming function (ADR-0028) and is no longer part of this Retrofit service —
 * see [RealVoiceGradingApi].
 */
interface VoiceGradingRetrofitService {

    @Multipart
    @POST("transcribe")
    suspend fun transcribe(@Part audio: MultipartBody.Part): TranscriptionDto

    @POST("sanitizeAndGrade")
    suspend fun sanitizeAndGrade(@Body request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto

    @GET("entitlement")
    suspend fun checkEntitlement(): EntitlementDto
}
