package com.rossomak.flashcards.data.mapper

import com.rossomak.flashcards.data.model.CategoryDto
import com.rossomak.flashcards.data.model.CodeBlockDto
import com.rossomak.flashcards.data.model.FlashcardDto
import com.rossomak.flashcards.data.model.SubcategoryDto
import com.rossomak.flashcards.domain.model.Category
import com.rossomak.flashcards.domain.model.CodeBlock
import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.model.Subcategory

fun CategoryDto.toDomain() = Category(
    id = id,
    name = name,
    order = order,
    subcategoryCount = subcategoryCount,
    iconUrl = iconUrl
)

fun SubcategoryDto.toDomain() = Subcategory(
    id = id,
    name = name,
    categoryId = categoryId,
    categoryName = categoryName,
    order = order,
    cardCount = cardCount
)

fun FlashcardDto.toDomain(subcategoryId: String) = Flashcard(
    id = id,
    subcategoryId = subcategoryId,
    question = question,
    answer = answer,
    tags = tags,
    difficulty = difficulty,
    questionCode = questionCode?.map { CodeBlock(it.language, it.code) },
    answerCode = answerCode?.map { CodeBlock(it.language, it.code) },
    questionSpoken = questionSpoken,
    answerSpoken = answerSpoken,
    extendedContext = extendedContext
)
