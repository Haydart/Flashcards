package com.rossomak.flashcards.presentation.main

data class MainScreenState(
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)
