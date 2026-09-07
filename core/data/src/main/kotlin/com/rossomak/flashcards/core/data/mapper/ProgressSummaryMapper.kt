package com.rossomak.flashcards.core.data.mapper

import com.rossomak.flashcards.core.data.model.ProgressSummaryDto
import com.rossomak.flashcards.core.domain.model.ProgressSummary
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressSummary

fun ProgressSummaryDto.toDomain(): ProgressSummary = ProgressSummary(
    subcategories = subcategories.mapValues { (_, dto) ->
        SubcategoryProgressSummary(masteredCount = dto.masteredCount, studiedCount = dto.studiedCount)
    },
)
