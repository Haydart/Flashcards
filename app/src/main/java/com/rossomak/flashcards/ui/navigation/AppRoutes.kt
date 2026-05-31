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
@Serializable data class HomeCategoryDetails(val categoryId: String)
@Serializable data class HomeSubcategoryDetails(val categoryId: String, val subcategoryId: String)

// Study tab destinations
@Serializable object StudyRoot

// Settings tab destinations
@Serializable object SettingsRoot
