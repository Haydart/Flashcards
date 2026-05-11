package com.rossomak.flashcards.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class SplashViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val NAVIGATION_TIMEOUT_MS = 5_000L
        private const val POST_ANIMATION_DELAY_MS = 2_000L
    }

    private val animationCompleted = MutableStateFlow(false)

    private val _navigationEvents = MutableSharedFlow<Unit>()
    val navigationEvents: SharedFlow<Unit> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            withTimeoutOrNull(NAVIGATION_TIMEOUT_MS) {
                animationCompleted.first { it }
                delay(POST_ANIMATION_DELAY_MS)
            }
            _navigationEvents.emit(Unit)
        }
    }

    fun onAnimationCompleted() {
        animationCompleted.value = true
    }
}
