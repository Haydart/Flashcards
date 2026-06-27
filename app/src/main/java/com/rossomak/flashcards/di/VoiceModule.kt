package com.rossomak.flashcards.di

import com.rossomak.flashcards.domain.voice.VoiceGateway
import com.rossomak.flashcards.data.voice.StudySessionVoiceGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class VoiceModule {

    @Binds
    @ViewModelScoped
    abstract fun bindVoiceGateway(impl: StudySessionVoiceGateway): VoiceGateway
}
