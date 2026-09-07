package com.rossomak.flashcards.core.data.di

import com.rossomak.flashcards.core.data.repository.DefaultAuthRepository
import com.rossomak.flashcards.core.data.repository.DefaultCardProgressRepository
import com.rossomak.flashcards.core.data.repository.DefaultCurationRepository
import com.rossomak.flashcards.core.data.repository.DefaultFlashcardRepository
import com.rossomak.flashcards.core.data.repository.DefaultStudySessionRepository
import com.rossomak.flashcards.core.data.source.AuthRemoteDataSource
import com.rossomak.flashcards.core.data.source.FirebaseAuthRemoteDataSource
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.repository.CardProgressRepository
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
import com.rossomak.flashcards.core.domain.repository.StudySessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(defaultAuthRepository: DefaultAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: FirebaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindFlashcardRepository(defaultFlashcardRepository: DefaultFlashcardRepository): FlashcardRepository

    @Binds
    @Singleton
    abstract fun bindCurationRepository(defaultCurationRepository: DefaultCurationRepository): CurationRepository

    @Binds
    @Singleton
    abstract fun bindStudySessionRepository(defaultStudySessionRepository: DefaultStudySessionRepository): StudySessionRepository

    @Binds
    @Singleton
    abstract fun bindCardProgressRepository(defaultCardProgressRepository: DefaultCardProgressRepository): CardProgressRepository
}
