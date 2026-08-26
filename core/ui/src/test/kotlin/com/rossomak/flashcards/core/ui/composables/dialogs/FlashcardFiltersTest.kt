package com.rossomak.flashcards.core.ui.composables.dialogs

import io.kotest.matchers.shouldBe
import org.junit.Test

class FlashcardFiltersTest {

    private val filters = FlashcardFilters(selectedTags = setOf("State"), difficultyRange = 1..5)

    @Test
    fun `withTag adds a tag that was not selected`() {
        filters.withTag("Modifiers", isSelected = true).selectedTags shouldBe setOf("State", "Modifiers")
    }

    @Test
    fun `withTag removes a selected tag`() {
        filters.withTag("State", isSelected = false).selectedTags shouldBe emptySet()
    }

    @Test
    fun `withTag selecting an already selected tag changes nothing`() {
        filters.withTag("State", isSelected = true).selectedTags shouldBe setOf("State")
    }

    @Test
    fun `withTag unselecting an absent tag changes nothing`() {
        filters.withTag("Modifiers", isSelected = false).selectedTags shouldBe setOf("State")
    }

    @Test
    fun `withTag leaves the difficulty range untouched`() {
        filters.withTag("Modifiers", isSelected = true).difficultyRange shouldBe 1..5
    }
}
