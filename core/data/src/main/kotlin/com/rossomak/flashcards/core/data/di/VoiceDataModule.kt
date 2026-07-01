package com.rossomak.flashcards.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.rossomak.flashcards.core.data.preview.DefaultVoicePreviewGateway
import com.rossomak.flashcards.core.data.repository.DefaultVoiceOptionsRepository
import com.rossomak.flashcards.core.data.repository.DefaultVoiceSettingsRepository
import com.rossomak.flashcards.core.domain.repository.VoiceOptionsRepository
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import com.rossomak.flashcards.core.domain.repository.VoiceSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.voiceDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_settings")

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceDataModule {

    @Binds
    @Singleton
    abstract fun bindVoiceSettingsRepository(
        impl: DefaultVoiceSettingsRepository,
    ): VoiceSettingsRepository

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

    companion object {

        @Provides
        @Singleton
        fun provideVoiceDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.voiceDataStore
    }
}
