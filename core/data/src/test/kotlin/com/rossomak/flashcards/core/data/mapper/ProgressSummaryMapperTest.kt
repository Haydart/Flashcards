package com.rossomak.flashcards.core.data.mapper

import com.rossomak.flashcards.core.data.model.ProgressSummaryDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressSummaryDto
import io.kotest.matchers.shouldBe
import org.junit.Test

class ProgressSummaryMapperTest {

    @Test
    fun `toDomain maps every subcategory's mastered and studied counts`() {
        val dto = ProgressSummaryDto(
            subcategories = mapOf(
                "sub-1" to SubcategoryProgressSummaryDto(masteredCount = 3, studiedCount = 5),
                "sub-2" to SubcategoryProgressSummaryDto(masteredCount = 0, studiedCount = 1),
            ),
        )

        val domain = dto.toDomain()

        domain.subcategories.keys shouldBe setOf("sub-1", "sub-2")
        domain.subcategories.getValue("sub-1").masteredCount shouldBe 3
        domain.subcategories.getValue("sub-1").studiedCount shouldBe 5
        domain.subcategories.getValue("sub-2").masteredCount shouldBe 0
        domain.subcategories.getValue("sub-2").studiedCount shouldBe 1
    }

    @Test
    fun `toDomain maps an empty subcategories map to an empty summary`() {
        val domain = ProgressSummaryDto().toDomain()

        domain.subcategories shouldBe emptyMap()
    }
}
