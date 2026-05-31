package com.rossomak.flashcards.presentation.study

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class StudyViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(StudyScreenState())
    val state: StateFlow<StudyScreenState> = _state.asStateFlow()
}
