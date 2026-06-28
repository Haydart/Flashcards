package com.rossomak.flashcards.core.domain.model

data class Flashcard(
    val id: String,
    val subcategoryId: String,
    val tags: List<String>,
    val question: String,
    val answer: String,
    val difficulty: Int,
    val questionCode: List<CodeBlock>?,
    val answerCode: List<CodeBlock>?,
    val questionSpoken: String?,
    val answerSpoken: String?,
    val extendedContext: String?
)
