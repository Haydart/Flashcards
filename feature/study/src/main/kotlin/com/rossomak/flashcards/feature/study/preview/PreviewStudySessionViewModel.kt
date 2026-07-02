package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.Flashcard
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.StudySessionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewStudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<PreviewStudySessionRoute>()

    private val _state = MutableStateFlow(
        PreviewStudySessionScreenState(
            categoryName = route.categoryName,
            subcategoryNames = route.subcategoryNames,
            isQuickSession = route.isQuickSession,
            filterTags = route.filterTagIds,
        )
    )
    val state: StateFlow<PreviewStudySessionScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<PreviewStudySessionDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var cardPool: List<Flashcard> = emptyList()
    private var selectedCards: List<Flashcard> = emptyList()

    init {
        loadCardPool()
    }

    fun onRetry() {
        loadCardPool()
    }

    fun onRerandomize() {
        selectCards()
    }

    fun onStudyModeSelect(mode: StudyMode) {
        _state.update { it.copy(selectedStudyMode = mode) }
    }

    fun onStartSession() {
        if (selectedCards.isEmpty()) return
        viewModelScope.launch {
            eventChannel.send(
                PreviewStudySessionDestination.StudySession(
                    StudySessionRoute(
                        categoryId = route.categoryId,
                        sessionTitle = sessionTitle(),
                        subcategoryIds = route.subcategoryIds,
                        cardIds = selectedCards.map { it.id },
                        studyMode = _state.value.selectedStudyMode,
                    )
                )
            )
        }
    }

    private fun loadCardPool() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val results = coroutineScope {
                route.subcategoryIds
                    .map { subcategoryId -> async { getFlashcards(subcategoryId) } }
                    .awaitAll()
            }
            if (results.any { it.isFailure }) {
                _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                return@launch
            }
            val allCards = results.flatMap { it.getOrThrow() }
            cardPool = if (route.filterTagIds.isEmpty()) {
                allCards
            } else {
                allCards.filter { card -> card.tags.any(route.filterTagIds::contains) }
            }
            selectCards()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun selectCards() {
        selectedCards = cardPool.shuffled().take(SESSION_CARD_COUNT)
        _state.update {
            it.copy(
                selectedCardCount = selectedCards.size,
                estimatedMinutes = estimateMinutes(selectedCards.size),
            )
        }
    }

    private fun sessionTitle(): String =
        if (_state.value.isSingleTopic) route.subcategoryNames.first() else route.categoryName

    private fun estimateMinutes(cardCount: Int): Int =
        ((cardCount * SECONDS_PER_CARD) + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE

    private companion object {
        // Session size default per SYSTEMDESIGN; user-configurable count is a future Settings feature.
        const val SESSION_CARD_COUNT = 20
        const val SECONDS_PER_CARD = 40
        const val SECONDS_PER_MINUTE = 60
    }
}
