package com.rossomak.flashcards.feature.auth

data class LoginScreenState(
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null,
    val navigationDestination: LoginDestination? = null
)
