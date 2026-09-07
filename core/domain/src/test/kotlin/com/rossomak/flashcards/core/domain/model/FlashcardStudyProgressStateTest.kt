package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class FlashcardStudyProgressStateTest {

    @Test
    fun `FlashcardTerminalRating Mastered maps to FlashcardStudyProgressState Mastered`() {
        FlashcardTerminalRating.Mastered.toFlashcardStudyProgressState() shouldBe FlashcardStudyProgressState.Mastered
    }

    @Test
    fun `FlashcardTerminalRating Partial maps to FlashcardStudyProgressState Partial`() {
        FlashcardTerminalRating.Partial.toFlashcardStudyProgressState() shouldBe FlashcardStudyProgressState.Partial
    }

    @Test
    fun `FlashcardTerminalRating Failed maps to FlashcardStudyProgressState Failed`() {
        FlashcardTerminalRating.Failed.toFlashcardStudyProgressState() shouldBe FlashcardStudyProgressState.Failed
    }
}
