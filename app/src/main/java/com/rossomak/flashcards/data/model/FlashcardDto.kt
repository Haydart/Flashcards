package com.rossomak.flashcards.data.model

data class FlashcardDto(
    val id: String = "",
    val question: String = "",
    val answer: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: String = "",
    val questionCode: List<CodeBlockDto>? = null,
    val answerCode: List<CodeBlockDto>? = null,
    val questionSpoken: String? = null,
    val answerSpoken: String? = null,
    val extendedContext: String? = null
)
