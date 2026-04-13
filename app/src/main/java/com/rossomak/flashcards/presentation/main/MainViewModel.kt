package com.rossomak.flashcards.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.domain.usecase.GetUserNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getUserNameUseCase: GetUserNameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    init {
        loadMessage()
    }

    private fun loadMessage() {
        viewModelScope.launch {
            getUserNameUseCase()
                .onSuccess { userName ->
                    _state.update { it.copy(isLoading = false, message = "Hello, $userName!") }
                }
                .onFailure { exception ->
                    _state.update { it.copy(isLoading = false, error = exception.message) }
                }
        }
    }
}
