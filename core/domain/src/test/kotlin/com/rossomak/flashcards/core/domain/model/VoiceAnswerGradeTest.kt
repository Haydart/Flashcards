package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class VoiceAnswerGradeTest {

    private fun grade(gradePercent: Int): VoiceAnswerGrade =
        VoiceAnswerGrade(sanitizedTranscript = "transcript", gradePercent = gradePercent, feedback = "feedback")

    @Test
    fun `a grade below 40 percent maps to Failed`() {
        grade(39).toFlashcardRating() shouldBe FlashcardRating.Failed
    }

    @Test
    fun `a grade of exactly 40 percent maps to PartiallyCorrect`() {
        grade(40).toFlashcardRating() shouldBe FlashcardRating.PartiallyCorrect
    }

    @Test
    fun `a grade of 79 percent maps to PartiallyCorrect`() {
        grade(79).toFlashcardRating() shouldBe FlashcardRating.PartiallyCorrect
    }

    @Test
    fun `a grade of exactly 80 percent maps to Correct`() {
        grade(80).toFlashcardRating() shouldBe FlashcardRating.Correct
    }

    @Test
    fun `a grade of 100 percent maps to Correct`() {
        grade(100).toFlashcardRating() shouldBe FlashcardRating.Correct
    }
}
