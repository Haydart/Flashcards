package com.rossomak.flashcards.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.model.AuthUser
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import com.rossomak.flashcards.domain.usecase.SignOutUseCase
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
class MainViewModel @Inject constructor(
    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private val _signedOutEvents = MutableSharedFlow<Unit>()
    val signedOutEvents: SharedFlow<Unit> = _signedOutEvents.asSharedFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = getCurrentAuthUserUseCase()
            if (user == null) {
                _signedOutEvents.emit(Unit)
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
        }
    }

    fun onSignOutClick() {
        viewModelScope.launch {
            signOutUseCase()
            _signedOutEvents.emit(Unit)
        }
    }

    private fun AuthUser.resolveDisplayName(): String {
        return displayName?.takeIf { it.isNotBlank() }
            ?: email?.takeIf { it.isNotBlank() }
            ?: "User"
    }
}
