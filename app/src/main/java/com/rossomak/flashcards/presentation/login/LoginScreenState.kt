package com.rossomak.flashcards.presentation.login

data class LoginScreenState(
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null,
    val navigationDestination: LoginDestination? = null
)
