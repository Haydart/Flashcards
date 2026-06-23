package com.rossomak.flashcards.domain.model

enum class CurationAction {
    DIFFICULTY_TOO_EASY,
    DIFFICULTY_TOO_HARD,
    DELETE,
    BACKTICK_REDO,
    NEEDS_CODE_EXAMPLE,
    FULL_REDO;

    fun difficultyOpposite(): CurationAction? = when (this) {
        DIFFICULTY_TOO_EASY -> DIFFICULTY_TOO_HARD
        DIFFICULTY_TOO_HARD -> DIFFICULTY_TOO_EASY
        else -> null
    }
}
