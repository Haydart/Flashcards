package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.Subcategory
import com.rossomak.flashcards.core.domain.usecase.GetCategoriesUseCase
import com.rossomak.flashcards.core.domain.usecase.SearchCategoriesParams
import com.rossomak.flashcards.core.domain.usecase.SearchCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val searchCategories: SearchCategoriesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseScreenState())
    val state: StateFlow<BrowseScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<BrowseNavigationDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadCategories()
        observeSearchQuery()
    }

    fun onCategoriesRefresh() {
        loadCategories()
    }

    fun onCategorySelected(categoryId: String, categoryName: String) {
        viewModelScope.launch {
            eventChannel.send(BrowseNavigationDestination.CategoryDetails(categoryId, categoryName))
        }
    }

    fun onSubcategorySelect(subcategory: Subcategory) {
        viewModelScope.launch {
            eventChannel.send(
                BrowseNavigationDestination.SubcategoryDetails(
                    categoryId = subcategory.categoryId,
                    categoryName = subcategory.categoryName,
                    subcategoryId = subcategory.id,
                    subcategoryName = subcategory.name,
                )
            )
        }
    }

    fun onSubcategorySessionStart(subcategory: Subcategory) {
        viewModelScope.launch {
            eventChannel.send(
                BrowseNavigationDestination.PreviewStudySession(
                    categoryId = subcategory.categoryId,
                    categoryName = subcategory.categoryName,
                    subcategoryId = subcategory.id,
                    subcategoryName = subcategory.name,
                )
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun onSearchActivate() {
        _state.update { it.copy(isSearchActive = true) }
    }

    /** Leaves search entirely. Clearing the text alone is the search field's own concern. */
    fun onSearchDismiss() {
        _state.update { it.copy(searchQuery = "", isSearchActive = false) }
    }

    /**
     * The search query lives in screen state rather than a separate input flow, so the text field
     * and this pipeline can't disagree about what was typed. `distinctUntilChanged` sits *before*
     * `debounce` so that unrelated state changes (a category load finishing, a navigation event)
     * don't restart the debounce timer for a query that never changed.
     */
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _state
                .map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .collectLatest { query -> runSearch(query) }
        }
    }

    private suspend fun runSearch(query: String) {
        if (query.trim().length < SearchCategoriesUseCase.MIN_QUERY_LENGTH) {
            _state.update { it.copy(searchResults = null, hasSearchError = false) }
            return
        }
        searchCategories(SearchCategoriesParams(query = query, categories = _state.value.categories))
            .onSuccess { results -> _state.update { it.copy(searchResults = results, hasSearchError = false) } }
            .onFailure { _state.update { it.copy(searchResults = null, hasSearchError = true) } }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasLoadError = false) }
            getCategories()
                .onSuccess { categories ->
                    _state.update { it.copy(isLoading = false, categories = categories) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, hasLoadError = true) }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 500L
    }
}
