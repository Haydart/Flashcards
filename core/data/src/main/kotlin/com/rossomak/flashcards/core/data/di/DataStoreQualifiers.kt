package com.rossomak.flashcards.core.data.di

import javax.inject.Qualifier

/**
 * The app keeps one `DataStore<Preferences>` per concern rather than a single shared store, so
 * every injection point has to say which one it wants. Without these qualifiers the second
 * `@Provides DataStore<Preferences>` would be a duplicate binding.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VoiceDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserPreferencesDataStore
