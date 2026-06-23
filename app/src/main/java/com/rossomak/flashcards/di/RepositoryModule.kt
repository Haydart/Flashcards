package com.rossomak.flashcards.di

import com.rossomak.flashcards.data.repository.AuthRepositoryImpl
import com.rossomak.flashcards.data.repository.CurationRepositoryImpl
import com.rossomak.flashcards.data.repository.FlashcardRepositoryImpl
import com.rossomak.flashcards.domain.repository.AuthRepository
import com.rossomak.flashcards.domain.repository.CurationRepository
import com.rossomak.flashcards.domain.repository.FlashcardRepository
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
    abstract fun bindAuthRepository(authRepositoryImpl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFlashcardRepository(flashcardRepositoryImpl: FlashcardRepositoryImpl): FlashcardRepository

    @Binds
    @Singleton
    abstract fun bindCurationRepository(curationRepositoryImpl: CurationRepositoryImpl): CurationRepository
}
