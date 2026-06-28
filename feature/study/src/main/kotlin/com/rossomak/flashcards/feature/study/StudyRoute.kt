package com.rossomak.flashcards.feature.study

import kotlinx.serialization.Serializable

@Serializable
data class StudyRoute(val subcategoryId: String, val subcategoryName: String)

@Serializable
data object StudySessionRoute

@Serializable
data object StudySummaryRoute
