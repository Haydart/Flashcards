package com.rossomak.flashcards.core.data.di

import com.rossomak.flashcards.core.data.BuildConfig
import com.rossomak.flashcards.core.data.network.FirebaseAuthTokenInterceptor
import com.rossomak.flashcards.core.data.network.VoiceGradingRetrofitService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Retrofit refuses a blank base URL; the placeholder is never hit because
    // VoiceGradingApiRouter only routes to the real client when a URL is configured.
    private const val UNCONFIGURED_BASE_URL = "https://voice-grading.invalid/"
    private const val CALL_TIMEOUT_SECONDS = 30L
    private const val JSON_MEDIA_TYPE = "application/json"

    @Provides
    @Singleton
    fun provideOkHttpClient(authTokenInterceptor: FirebaseAuthTokenInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authTokenInterceptor)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val configuredBaseUrl = BuildConfig.VOICE_GRADING_BASE_URL
            .takeIf { it.isNotBlank() }
            ?.let { "${it.trimEnd('/')}/" }
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(configuredBaseUrl ?: UNCONFIGURED_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideVoiceGradingRetrofitService(retrofit: Retrofit): VoiceGradingRetrofitService =
        retrofit.create(VoiceGradingRetrofitService::class.java)
}
