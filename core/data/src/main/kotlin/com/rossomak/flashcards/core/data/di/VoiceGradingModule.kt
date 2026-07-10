package com.rossomak.flashcards.core.data.di

import com.rossomak.flashcards.core.data.network.VoiceGradingApi
import com.rossomak.flashcards.core.data.network.VoiceGradingApiRouter
import com.rossomak.flashcards.core.data.repository.DefaultVoiceAnswerConsentRepository
import com.rossomak.flashcards.core.data.repository.DefaultVoiceAnswerGradingRepository
import com.rossomak.flashcards.core.data.source.DataStoreVoiceAnswerConsentLocalDataSource
import com.rossomak.flashcards.core.data.source.VoiceAnswerConsentLocalDataSource
import com.rossomak.flashcards.core.domain.repository.VoiceAnswerConsentRepository
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
    abstract fun bindVoiceGradingApi(impl: VoiceGradingApiRouter): VoiceGradingApi

    @Binds
    @Singleton
    abstract fun bindVoiceAnswerGradingRepository(
        impl: DefaultVoiceAnswerGradingRepository,
    ): VoiceAnswerGradingRepository

    @Binds
    @Singleton
    abstract fun bindVoiceAnswerConsentRepository(
        impl: DefaultVoiceAnswerConsentRepository,
    ): VoiceAnswerConsentRepository

    @Binds
    @Singleton
    abstract fun bindVoiceAnswerConsentLocalDataSource(
        impl: DataStoreVoiceAnswerConsentLocalDataSource,
    ): VoiceAnswerConsentLocalDataSource
}
