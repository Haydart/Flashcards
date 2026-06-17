package com.rossomak.flashcards.presentation.studysession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rossomak.flashcards.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.ui.navigation.StudySession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<StudySession>()

    private val _state = MutableStateFlow(StudySessionScreenState(subcategoryName = route.subcategoryName))
    val state: StateFlow<StudySessionScreenState> = _state.asStateFlow()

    init {
        loadFlashcards()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getFlashcards(route.subcategoryId)
                .onSuccess { flashcards ->
                    val sampleSize = minOf((flashcards.size * 0.6).toInt(), 100)
                    val sampled = flashcards.shuffled().take(sampleSize)
                    _state.update { it.copy(isLoading = false, flashcards = sampled) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                }
        }
    }

    fun onShowAnswer() {
        _state.update { it.copy(isAnswerRevealed = true) }
    }

    fun onNextCard() {
        val currentState = _state.value
        if (currentState.currentCardIndex >= currentState.flashcards.lastIndex) {
            _state.update { it.copy(isSessionComplete = true) }
        } else {
            _state.update {
                it.copy(
                    currentCardIndex = it.currentCardIndex + 1,
                    isAnswerRevealed = false,
                )
            }
        }
    }
}
