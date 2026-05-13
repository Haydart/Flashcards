package com.rossomak.flashcards.presentation.main

data class MainScreenState(
    val isLoading: Boolean = true,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val error: String? = null
)
