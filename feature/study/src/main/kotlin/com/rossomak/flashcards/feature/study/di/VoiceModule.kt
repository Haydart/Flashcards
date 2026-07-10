package com.rossomak.flashcards.feature.study.di

import com.rossomak.flashcards.feature.study.voice.StudySessionVoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
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
