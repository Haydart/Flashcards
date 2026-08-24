package com.rossomak.flashcards.core.domain.model

enum class CurationAction {
    DifficultyTooEasy,
    DifficultyTooHard,
    Delete,
    BacktickRedo,
    WrongTags,
    NeedsCodeExample,
    FullRedo;

    fun difficultyOpposite(): CurationAction? = when (this) {
        DifficultyTooEasy -> DifficultyTooHard
        DifficultyTooHard -> DifficultyTooEasy
        else -> null
    }
}
