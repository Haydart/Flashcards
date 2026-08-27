package com.rossomak.flashcards.core.data.di

import com.rossomak.flashcards.core.data.preview.DefaultVoicePreviewGateway
import com.rossomak.flashcards.core.data.repository.DefaultVoiceOptionsRepository
import com.rossomak.flashcards.core.domain.repository.VoiceOptionsRepository
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceDataModule {

    @Binds
    @Singleton
    abstract fun bindVoiceOptionsRepository(
        impl: DefaultVoiceOptionsRepository,
    ): VoiceOptionsRepository

    @Binds
    @Singleton
    abstract fun bindVoicePreviewGateway(
        impl: DefaultVoicePreviewGateway,
    ): VoicePreviewGateway
}
