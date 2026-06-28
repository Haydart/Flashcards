package com.rossomak.flashcards.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetCurrentAuthUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentAuthUser: GetCurrentAuthUserUseCase
) : ViewModel() {

    private val _animationCompleted = MutableStateFlow(false)
    private val _authenticated = MutableStateFlow<Boolean?>(null)

    val navigationDestination: StateFlow<SplashDestination?> = combine(
        _animationCompleted,
        _authenticated
    ) { animDone, authenticated ->
        if (animDone && authenticated != null) {
            if (authenticated) SplashDestination.Main else SplashDestination.Login
        } else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val authenticated = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                getCurrentAuthUser() != null
            } ?: false
            _authenticated.value = authenticated
        }
    }

    fun onAnimationCompleted() {
        _animationCompleted.value = true
    }

    private companion object {
        const val AUTH_TIMEOUT_MS = 1000L
    }
}
