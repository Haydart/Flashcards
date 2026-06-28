package com.rossomak.flashcards.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetCurrentAuthUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class AppStartViewModel @Inject constructor(
    private val getCurrentAuthUser: GetCurrentAuthUserUseCase
) : ViewModel() {

    val startupState: StateFlow<AppStartupState> =
        flow {
            val authenticated = withTimeoutOrNull(STARTUP_AUTH_TIMEOUT_MS) {
                getCurrentAuthUser() != null
            } ?: false
            emit(AppStartupState.Ready(authenticated = authenticated))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppStartupState.Loading
        )

    private companion object {
        const val STARTUP_AUTH_TIMEOUT_MS = 800L
    }
}
