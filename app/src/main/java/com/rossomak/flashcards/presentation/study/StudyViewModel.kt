package com.rossomak.flashcards.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StudyScreenState())
    val state: StateFlow<StudyScreenState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun onCategoriesRefresh() {
        loadCategories()
    }

    fun onCategorySelected(categoryId: String, categoryName: String) {
        _state.update { it.copy(navigationDestination = StudyDestination.CategoryDetails(categoryId, categoryName)) }
    }

    fun onNavigationHandled() {
        _state.update { it.copy(navigationDestination = null) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getCategories()
                .onSuccess { categories ->
                    _state.update { it.copy(isLoading = false, categories = categories) }
                }
                .onFailure { throwable ->
                    _state.update { it.copy(isLoading = false, error = "Could not load categories") }
                }
        }
    }
}
