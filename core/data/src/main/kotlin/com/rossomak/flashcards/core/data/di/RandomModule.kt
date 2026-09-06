package com.rossomak.flashcards.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.random.Random

/**
 * Binds [Random.Default] for every use case that shuffles or samples without a session seed
 * (ADR-0040) — Dagger can't fall back to a Kotlin default parameter value on an `@Inject`
 * constructor (it rejects the synthetic defaults constructor as a second `@Inject` constructor),
 * so production wiring goes through this binding instead. Unit tests never see it: they construct
 * the use case directly with a fixed `Random`, bypassing Hilt entirely.
 */
@Module
@InstallIn(SingletonComponent::class)
object RandomModule {

    @Provides
    fun provideRandom(): Random = Random.Default
}
