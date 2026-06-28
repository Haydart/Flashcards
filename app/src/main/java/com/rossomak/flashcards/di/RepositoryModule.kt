package com.rossomak.flashcards.di

import com.rossomak.flashcards.data.repository.DefaultCurationRepository
import com.rossomak.flashcards.data.repository.DefaultAuthRepository
import com.rossomak.flashcards.data.repository.DefaultFlashcardRepository
import com.rossomak.flashcards.core.domain.repository.AuthRepository
import com.rossomak.flashcards.core.domain.repository.CurationRepository
import com.rossomak.flashcards.core.domain.repository.FlashcardRepository
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
    abstract fun bindFlashcardRepository(defaultFlashcardRepository: DefaultFlashcardRepository): FlashcardRepository

    @Binds
    @Singleton
    abstract fun bindCurationRepository(defaultCurationRepository: DefaultCurationRepository): CurationRepository
}
