package com.rossomak.flashcards.feature.browse

import androidx.compose.runtime.Immutable
import com.rossomak.flashcards.core.domain.model.Category
import com.rossomak.flashcards.core.domain.model.CategorySearchResults
import com.rossomak.flashcards.core.ui.navigation.NavigationEvent

sealed interface BrowseNavigationDestination : NavigationEvent {
    data class CategoryDetails(val categoryId: String, val categoryName: String) : BrowseNavigationDestination

    data class SubcategoryDetails(
        val categoryId: String,
        val categoryName: String,
        val subcategoryId: String,
        val subcategoryName: String,
    ) : BrowseNavigationDestination

    /** Straight from a search result's play button into Study Creation, skipping Subcategory Details. */
    data class PreviewStudySession(
        val categoryId: String,
        val categoryName: String,
        val subcategoryId: String,
        val subcategoryName: String,
    ) : BrowseNavigationDestination
}

/**
 * The search field's callbacks travel together as one parameter — they are a single concern
 * and always wired to the same ViewModel. Marked [Immutable] so Compose treats the holder as
 * stable; callers should `remember` it rather than rebuilding it on every recomposition.
 *
 * There is no `onClear`: the Material 3 search field owns its own text, so clearing it is a
 * `TextFieldState` operation that reaches the ViewModel through [onQueryChange] like any other
 * edit.
 */
@Immutable
data class BrowseSearchActions(
    val onQueryChange: (String) -> Unit,
    val onActivate: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * [searchResults] is the switch between the two things the expanded search bar can show: `null`
 * means no query has been made yet (or one too short to run), non-null means render the sections —
 * including when both are empty, which is the "no matches" state.
 *
 * [isSearchActive] records whether search is open. The expanded/collapsed state the UI actually
 * renders from is Material 3's own `SearchBarState`; this mirrors it so the ViewModel can clear the
 * query on dismissal without reaching into the UI.
 *
 * [hasSearchError] is kept separate from [hasLoadError] because the two fail differently:
 * [hasLoadError] means the category list itself could not load and there is nothing to show, while
 * [hasSearchError] leaves the loaded categories intact and only reports that one query failed.
 * Both are flags rather than messages so the display copy stays in the UI layer as string
 * resources, not as hardcoded text in the ViewModel.
 */
data class BrowseScreenState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val hasLoadError: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchResults: CategorySearchResults? = null,
    val hasSearchError: Boolean = false,
)
