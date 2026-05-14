package com.rossomak.flashcards.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase
) : ViewModel() {

    companion object {
        private const val MAX_SPLASH_NAVIGATION_TIMEOUT_MS = 5_000L
        private const val POST_ANIMATION_DELAY_MS = 2_000L
    }

    private val animationCompleted = MutableStateFlow(false)

    private val _state = MutableStateFlow(SplashScreenState())
    val state: StateFlow<SplashScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            withTimeoutOrNull(MAX_SPLASH_NAVIGATION_TIMEOUT_MS) {
                animationCompleted.first { it }
                delay(POST_ANIMATION_DELAY_MS)
            }
            val destination = if (getCurrentAuthUserUseCase() != null) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
            _state.update { it.copy(navigationDestination = destination) }
        }
    }

    fun onAnimationCompleted() {
        animationCompleted.value = true
    }
}
