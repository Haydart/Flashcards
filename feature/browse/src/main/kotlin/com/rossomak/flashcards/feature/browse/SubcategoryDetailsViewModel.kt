package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubcategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<SubcategoryDetailsRoute>()

    private val _state = MutableStateFlow(
        SubcategoryDetailsScreenState(
            categoryName = route.categoryName,
            subcategoryName = route.subcategoryName
        )
    )
    val state: StateFlow<SubcategoryDetailsScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<SubcategoryDetailsDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadFlashcards()
    }

    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getFlashcards(route.subcategoryId)
                .onSuccess { flashcards ->
                    _state.update { it.copy(isLoading = false, flashcards = flashcards) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                }
        }
    }

    fun onStartSession() {
        viewModelScope.launch {
            eventChannel.send(
                SubcategoryDetailsDestination.StudySession(
                    route.subcategoryId,
                    route.subcategoryName
                )
            )
        }
    }
}
