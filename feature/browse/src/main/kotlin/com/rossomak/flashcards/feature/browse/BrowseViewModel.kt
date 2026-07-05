package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetCategoriesUseCase
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
class BrowseViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseScreenState())
    val state: StateFlow<BrowseScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<BrowseNavigationDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadCategories()
    }

    fun onCategoriesRefresh() {
        loadCategories()
    }

    fun onCategorySelected(categoryId: String, categoryName: String) {
        viewModelScope.launch {
            eventChannel.send(BrowseNavigationDestination.CategoryDetails(categoryId, categoryName))
        }
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
