package com.rossomak.flashcards.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import com.rossomak.flashcards.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val user = getCurrentAuthUserUseCase()
                if (user == null) {
                    _state.update { it.copy(navigationDestination = MainDestination.Login) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        displayName = user.resolveDisplayName(),
                        photoUrl = user.photoUrl,
                        error = null
                    )
                }
            } catch (e: Exception) {
                // TODO: push load failure event to analytics
            }
        }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            try {
                signOutUseCase()
            } catch (e: Exception) {
                // TODO: push sign-out failure event to analytics
            } finally {
                _state.update { it.copy(navigationDestination = MainDestination.Login) }
            }
        }
    }

    private fun AuthUser.resolveDisplayName(): String {
        return displayName?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: "User"
    }
}
