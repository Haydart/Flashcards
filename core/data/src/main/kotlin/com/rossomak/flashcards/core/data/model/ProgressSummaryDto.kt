package com.rossomak.flashcards.core.data.model

data class ProgressSummaryDto(
    val subcategories: Map<String, SubcategoryProgressSummaryDto> = emptyMap(),
)

data class SubcategoryProgressSummaryDto(
    val masteredCount: Int = 0,
    val studiedCount: Int = 0,
)
