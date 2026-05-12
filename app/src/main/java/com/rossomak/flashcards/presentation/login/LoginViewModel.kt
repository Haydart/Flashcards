package com.rossomak.flashcards.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.usecase.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _navigationEvents = MutableSharedFlow<Unit>()
    val navigationEvents: SharedFlow<Unit> = _navigationEvents.asSharedFlow()

    fun onSignInStarted() {
        _state.update { it.copy(isSigningIn = true, errorMessage = null) }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _state.update { it.copy(isSigningIn = false, errorMessage = null) }
                    _navigationEvents.emit(Unit)
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
