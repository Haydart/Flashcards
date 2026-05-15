package com.rossomak.flashcards.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.usecase.GetCurrentAuthUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val getCurrentAuthUserUseCase: GetCurrentAuthUserUseCase
) : ViewModel() {

    companion object {
        private const val NAVIGATION_TIMEOUT_MS = 5_000L
        private const val POST_ANIMATION_DELAY_MS = 2_000L
    }

    private val animationCompleted = MutableStateFlow(false)

    private val _navigationEvents = MutableSharedFlow<SplashDestination>()
    val navigationEvents: SharedFlow<SplashDestination> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            withTimeoutOrNull(NAVIGATION_TIMEOUT_MS) {
                animationCompleted.first { it }
                delay(POST_ANIMATION_DELAY_MS)
            }
            val destination = if (getCurrentAuthUserUseCase() != null) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
            _navigationEvents.emit(destination)
        }
    }

    fun onAnimationCompleted() {
        animationCompleted.value = true
    }
}
