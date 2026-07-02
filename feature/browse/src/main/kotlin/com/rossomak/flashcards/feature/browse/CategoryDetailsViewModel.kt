package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetSubcategoriesUseCase
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubcategories: GetSubcategoriesUseCase,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<CategoryDetailsRoute>()

    private val _state = MutableStateFlow(CategoryDetailsScreenState(categoryId = route.categoryId, categoryName = route.categoryName))
    val state: StateFlow<CategoryDetailsScreenState> = _state.asStateFlow()

    init {
        loadSubcategories()
    }

    private fun loadSubcategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getSubcategories(route.categoryId)
                .onSuccess { subcategories ->
                    _state.update { it.copy(isLoading = false, subcategories = subcategories) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load topics") }
                }
        }
    }
}
