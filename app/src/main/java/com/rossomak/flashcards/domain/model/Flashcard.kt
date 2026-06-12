package com.rossomak.flashcards.domain.model

data class Flashcard(
    val id: String,
    val subcategoryId: String,
    val tags: List<String>,
    val question: String,
    val answer: String,
    val questionCode: String?,
    val answerCode: String?,
    val questionSpoken: String?,
    val answerSpoken: String?,
    val extendedContext: String?
)
