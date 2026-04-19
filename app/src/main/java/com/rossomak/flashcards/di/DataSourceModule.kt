package com.rossomak.flashcards.di

import com.rossomak.flashcards.data.datasource.MockUserDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideMockUserDataSource(): MockUserDataSource {
        return MockUserDataSource()
    }
}
