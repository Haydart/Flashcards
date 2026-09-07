package com.rossomak.flashcards.core.domain.repository

import com.rossomak.flashcards.core.domain.model.SubcategoryProgress

class FakeCardProgressRepository : CardProgressRepository {
    private val progressBySubcategoryId: MutableMap<String, SubcategoryProgress> = mutableMapOf()

    /** Overrides every [getProgress] call when set, success or failure alike. */
    var resultToReturn: Result<SubcategoryProgress?>? = null

    /** Every Subcategory id [getProgress] was actually called with, in call order. */
    val requestedSubcategoryIds: MutableList<String> = mutableListOf()

    fun seed(progress: SubcategoryProgress) {
        progressBySubcategoryId[progress.subcategoryId] = progress
    }

    override suspend fun getProgress(subcategoryId: String): Result<SubcategoryProgress?> {
        requestedSubcategoryIds.add(subcategoryId)
        return resultToReturn ?: Result.success(progressBySubcategoryId[subcategoryId])
    }
}
