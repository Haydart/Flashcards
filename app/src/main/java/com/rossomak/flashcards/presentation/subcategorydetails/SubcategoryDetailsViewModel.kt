package com.rossomak.flashcards.presentation.subcategorydetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rossomak.flashcards.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.ui.navigation.SubcategoryDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubcategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SubcategoryDetails>()

    private val _state = MutableStateFlow(
        SubcategoryDetailsScreenState(
            categoryName = route.categoryName,
            subcategoryName = route.subcategoryName
        )
    )
    val state: StateFlow<SubcategoryDetailsScreenState> = _state.asStateFlow()

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
        _state.update {
            it.copy(
                navigationDestination = SubcategoryDetailsDestination.StudySession(
                    route.subcategoryId,
                    route.subcategoryName
                )
            )
        }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigationDestination = null) }
    }
}
