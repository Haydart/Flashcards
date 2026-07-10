package com.rossomak.flashcards.core.data.network

import com.rossomak.flashcards.core.data.model.EntitlementDto
import com.rossomak.flashcards.core.data.model.SanitizeAndGradeRequestDto
import com.rossomak.flashcards.core.data.model.TranscriptionDto
import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Retrofit binding for the Cloud Function proxy endpoints. */
interface VoiceGradingRetrofitService {

    @Multipart
    @POST("gradeVoiceAnswer")
    suspend fun gradeVoiceAnswer(
        @Part audio: MultipartBody.Part,
        @Part("card_id") cardId: RequestBody,
        @Part("question") question: RequestBody,
        @Part("expected_answer") expectedAnswer: RequestBody,
    ): VoiceAnswerGradeDto

    @Multipart
    @POST("transcribe")
    suspend fun transcribe(@Part audio: MultipartBody.Part): TranscriptionDto

    @POST("sanitizeAndGrade")
    suspend fun sanitizeAndGrade(@Body request: SanitizeAndGradeRequestDto): VoiceAnswerGradeDto

    @GET("entitlement")
    suspend fun checkEntitlement(): EntitlementDto
}
