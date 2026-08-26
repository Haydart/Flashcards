package com.rossomak.flashcards.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.rossomak.flashcards.core.domain.model.FlashcardRating

/**
 * Container/content pair per [FlashcardRating]. Deliberately identical in both themes — like
 * [DifficultyColors], these encode a rating value rather than a themed surface, so they never flip
 * light/dark. Every surface that shows a rating (the self-rating buttons in a Rated session, the
 * onboarding illustration, the read-only grade badge in Voice Answering) reads them from here so
 * the three colours mean the same thing everywhere.
 */
object RatingColors {

    private val failedContainer = Color(0xFFF6D9DA)
    private val failedContent = Color(0xFFC94F4F)
    private val partiallyCorrectContainer = Color(0xFFF6E8C8)
    private val partiallyCorrectContent = Color(0xFF9F7122)
    private val correctContainer = Color(0xFFD3EBD6)
    private val correctContent = Color(0xFF38874E)

    fun containerColorFor(rating: FlashcardRating): Color = when (rating) {
        FlashcardRating.Failed -> failedContainer
        FlashcardRating.PartiallyCorrect -> partiallyCorrectContainer
        FlashcardRating.Correct -> correctContainer
    }

    fun contentColorFor(rating: FlashcardRating): Color = when (rating) {
        FlashcardRating.Failed -> failedContent
        FlashcardRating.PartiallyCorrect -> partiallyCorrectContent
        FlashcardRating.Correct -> correctContent
    }
}

val MaterialTheme.ratingColors: RatingColors
    get() = RatingColors
