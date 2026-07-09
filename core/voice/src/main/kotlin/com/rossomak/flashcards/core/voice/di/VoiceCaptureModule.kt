package com.rossomak.flashcards.core.voice.di

import android.content.Context
import com.rossomak.flashcards.core.voice.OnnxSileroVadSession
import com.rossomak.flashcards.core.voice.PitchShiftVoiceObfuscator
import com.rossomak.flashcards.core.voice.SileroVadSession
import com.rossomak.flashcards.core.voice.SileroVoiceActivityDetector
import com.rossomak.flashcards.core.voice.VoiceActivityDetector
import com.rossomak.flashcards.core.voice.VoiceObfuscator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceCaptureModule {

    @Binds
    @Singleton
    abstract fun bindVoiceActivityDetector(impl: SileroVoiceActivityDetector): VoiceActivityDetector

    @Binds
    @Singleton
    abstract fun bindVoiceObfuscator(impl: PitchShiftVoiceObfuscator): VoiceObfuscator

    companion object {

        private const val SILERO_MODEL_ASSET = "silero_vad.onnx"

        @Provides
        @Singleton
        fun provideSileroVadSession(@ApplicationContext context: Context): SileroVadSession =
            OnnxSileroVadSession(context.assets.open(SILERO_MODEL_ASSET).use { it.readBytes() })
    }
}
