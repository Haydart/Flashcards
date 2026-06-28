package com.rossomak.flashcards.ui.navigation

import kotlinx.serialization.Serializable

// Root destinations
@Serializable object Splash
@Serializable object Login
@Serializable object Main

// Tab graph containers
@Serializable object HomeGraph
@Serializable object StudyGraph
@Serializable object SettingsGraph

// Home tab destinations
@Serializable object HomeRoot

// Study tab destinations
@Serializable object StudyRoot

// Settings tab destinations
@Serializable object SettingsRoot

// Shared full-screen destinations (no bottom nav)
@Serializable data class StudySession(val subcategoryId: String, val subcategoryName: String)