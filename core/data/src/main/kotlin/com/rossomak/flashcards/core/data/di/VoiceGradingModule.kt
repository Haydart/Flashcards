package com.rossomak.flashcards.core.data.di

import com.rossomak.flashcards.core.data.network.RealVoiceGradingApi
import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.data.repository.DefaultVoiceAnswerGradingRepository
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerGradingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceGradingModule {

    @Binds
    @Singleton
    abstract fun bindVoiceGradingApi(impl: RealVoiceGradingApi): VoiceGradingApi

    @Binds
    @Singleton
    abstract fun bindVoiceAnswerGradingRepository(
        impl: DefaultVoiceAnswerGradingRepository,
    ): VoiceAnswerGradingRepository
}
