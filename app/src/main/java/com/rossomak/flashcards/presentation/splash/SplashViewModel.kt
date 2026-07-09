package com.rossomak.flashcards.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetCurrentAuthUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(private val getCurrentAuthUser: GetCurrentAuthUserUseCase) : ViewModel() {

    private val _animationCompleted = MutableStateFlow(false)
    private val _authenticated = MutableStateFlow<Boolean?>(null)

    private val eventChannel = Channel<SplashDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val authenticated = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                getCurrentAuthUser() != null
            } ?: false
            _authenticated.value = authenticated
        }
        viewModelScope.launch {
            val destination = combine(_animationCompleted, _authenticated) { animDone, authenticated ->
                if (animDone && authenticated != null) {
                    if (authenticated) SplashDestination.Main else SplashDestination.Login
                } else {
                    null
                }
            }.filterNotNull().first()
            eventChannel.send(destination)
        }
    }

    fun onAnimationCompleted() {
        _animationCompleted.value = true
    }

    private companion object {
        const val AUTH_TIMEOUT_MS = 1000L
    }
}
