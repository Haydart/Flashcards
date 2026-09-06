package com.rossomak.flashcards.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.Test

class FlashcardProgressStateTest {

    @Test
    fun `TerminalState Mastered maps to FlashcardProgressState Mastered`() {
        TerminalState.Mastered.toFlashcardProgressState() shouldBe FlashcardProgressState.Mastered
    }

    @Test
    fun `TerminalState Partial maps to FlashcardProgressState Partial`() {
        TerminalState.Partial.toFlashcardProgressState() shouldBe FlashcardProgressState.Partial
    }

    @Test
    fun `TerminalState Failed maps to FlashcardProgressState Failed`() {
        TerminalState.Failed.toFlashcardProgressState() shouldBe FlashcardProgressState.Failed
    }
}
