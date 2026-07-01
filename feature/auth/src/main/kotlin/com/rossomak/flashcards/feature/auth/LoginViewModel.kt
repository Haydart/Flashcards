package com.rossomak.flashcards.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginScreenState())
    val state: StateFlow<LoginScreenState> = _state.asStateFlow()

    fun onSignInStarted() {
        _state.update { it.copy(isSigningIn = true, errorMessage = null) }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _state.update { it.copy(isSigningIn = false, errorMessage = null, navigationDestination = LoginDestination.Main) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSigningIn = false,
                            errorMessage = error.message ?: "Sign-in failed"
                        )
                    }
                }
        }
    }

    fun onSignInFailed(message: String?) {
        _state.update {
            it.copy(
                isSigningIn = false,
                errorMessage = message ?: "Sign-in failed"
            )
        }
    }
}
