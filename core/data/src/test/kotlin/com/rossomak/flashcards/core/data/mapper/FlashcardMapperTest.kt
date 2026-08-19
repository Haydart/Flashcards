package com.rossomak.flashcards.core.data.mapper

import com.rossomak.flashcards.core.data.model.CategoryDto
import io.kotest.matchers.shouldBe
import org.junit.Test

class FlashcardMapperTest {

    @Test
    fun `CategoryDto toDomain passes through color and iconSvg unchanged when present`() {
        val dto = CategoryDto(
            id = "android",
            name = "Android",
            order = 0,
            subcategoryCount = 13,
            iconSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\"><path d=\"M0,0\"/></svg>",
            color = "#6B2FA0",
        )

        val domain = dto.toDomain()

        domain.iconSvg shouldBe dto.iconSvg
        domain.color shouldBe dto.color
    }

    @Test
    fun `CategoryDto toDomain passes through null color and iconSvg unchanged`() {
        val dto = CategoryDto(id = "android", name = "Android", order = 0, subcategoryCount = 13)

        val domain = dto.toDomain()

        domain.iconSvg shouldBe null
        domain.color shouldBe null
    }
}
