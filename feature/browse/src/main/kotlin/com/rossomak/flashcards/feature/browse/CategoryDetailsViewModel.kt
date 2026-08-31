package com.rossomak.flashcards.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.usecase.GetSubcategoriesUseCase
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubcategories: GetSubcategoriesUseCase,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<CategoryDetailsRoute>()

    private val _state = MutableStateFlow(CategoryDetailsScreenState(categoryId = route.categoryId, categoryName = route.categoryName))
    val state: StateFlow<CategoryDetailsScreenState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<CategoryDetailsMessage>(extraBufferCapacity = 1)

    /** Transient one-shot messages for the snackbar. Never screen state. */
    val messages: SharedFlow<CategoryDetailsMessage> = _messages.asSharedFlow()

    init {
        loadSubcategories()
    }

    /**
     * Deliberately fake, exactly like [SubcategoryDetailsViewModel.onFavoriteToggle]. This flips a
     * flag that dies with the ViewModel and shows a snackbar, and **writes nothing anywhere** — no
     * repository, no use case, no preference. Do not wire it to storage on the assumption that it
     * is a half-finished integration; making favourites real is its own piece of work.
     */
    fun onFavoriteToggle() {
        val isFavorite = !_state.value.isFavorite
        _state.update { it.copy(isFavorite = isFavorite) }
        _messages.tryEmit(
            if (isFavorite) CategoryDetailsMessage.AddedToFavorites else CategoryDetailsMessage.RemovedFromFavorites
        )
    }

    /**
     * Undo on the favourite snackbar. Restores the value the toggle moved away from rather than
     * flipping whatever is current: a snackbar outlives the tap that raised it, so a blind flip
     * would invert a later, unrelated toggle. Emits no message of its own.
     */
    fun onFavoriteUndo(restoreTo: Boolean) {
        _state.update { it.copy(isFavorite = restoreTo) }
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
