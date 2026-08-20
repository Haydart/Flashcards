package com.rossomak.flashcards.core.data.repository

import com.rossomak.flashcards.core.data.model.CategoryDto
import com.rossomak.flashcards.core.data.model.FlashcardDto
import com.rossomak.flashcards.core.data.model.SubcategoryDto
import com.rossomak.flashcards.core.data.source.FlashcardRemoteDataSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFlashcardRepositoryTest {

    private val remoteDataSource: FlashcardRemoteDataSource = mockk()

    private fun createRepository(): DefaultFlashcardRepository =
        DefaultFlashcardRepository(remoteDataSource)

    @Test
    fun `fetchCategories maps dtos to domain in order`() = runTest {
        val categoryDto = CategoryDto(id = "cat-1", name = "Android", order = 2, subcategoryCount = 3, iconSvg = "<svg />", color = "#6B2FA0")
        coEvery { remoteDataSource.getCategories() } returns listOf(categoryDto)

        val result = createRepository().fetchCategories()

        result.isSuccess shouldBe true
        val category = result.getOrThrow().single()
        category.id shouldBe categoryDto.id
        category.name shouldBe categoryDto.name
        category.order shouldBe categoryDto.order
        category.subcategoryCount shouldBe categoryDto.subcategoryCount
        category.iconSvg shouldBe categoryDto.iconSvg
        category.color shouldBe categoryDto.color
        coVerify(exactly = 1) { remoteDataSource.getCategories() }
    }

    @Test
    fun `fetchCategories wraps data source failure in failure result`() = runTest {
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getCategories() } throws error

        val result = createRepository().fetchCategories()

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getCategories() }
    }

    @Test
    fun `fetchCategories rethrows cancellation instead of wrapping it`() = runTest {
        coEvery { remoteDataSource.getCategories() } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().fetchCategories() }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { remoteDataSource.getCategories() }
    }

    @Test
    fun `fetchSubcategories forwards category id and maps dtos to domain`() = runTest {
        val categoryId = "cat-1"
        val subcategoryDto = SubcategoryDto(
            id = "sub-1",
            name = "Compose",
            categoryId = categoryId,
            categoryName = "Android",
            order = 1,
            cardCount = 12,
        )
        coEvery { remoteDataSource.getSubcategoriesByCategoryId(categoryId) } returns listOf(subcategoryDto)

        val result = createRepository().fetchSubcategories(categoryId)

        result.isSuccess shouldBe true
        val subcategory = result.getOrThrow().single()
        subcategory.id shouldBe subcategoryDto.id
        subcategory.categoryId shouldBe categoryId
        subcategory.cardCount shouldBe subcategoryDto.cardCount
        coVerify(exactly = 1) { remoteDataSource.getSubcategoriesByCategoryId(categoryId) }
    }

    @Test
    fun `fetchSubcategories wraps data source failure in failure result`() = runTest {
        val categoryId = "cat-1"
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getSubcategoriesByCategoryId(categoryId) } throws error

        val result = createRepository().fetchSubcategories(categoryId)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getSubcategoriesByCategoryId(categoryId) }
    }

    @Test
    fun `fetchFlashcards drops cards with null difficulty and forwards subcategory id`() = runTest {
        val subcategoryId = "sub-1"
        val ratedDto = FlashcardDto(id = "card-1", question = "q", answer = "a", difficulty = 4)
        val unratedDto = FlashcardDto(id = "card-2", question = "q2", answer = "a2", difficulty = null)
        coEvery { remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId) } returns listOf(ratedDto, unratedDto)

        val result = createRepository().fetchFlashcards(subcategoryId)

        result.isSuccess shouldBe true
        val flashcard = result.getOrThrow().single()
        flashcard.id shouldBe ratedDto.id
        flashcard.subcategoryId shouldBe subcategoryId
        flashcard.difficulty shouldBe 4
        coVerify(exactly = 1) { remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId) }
    }

    @Test
    fun `fetchFlashcards wraps data source failure in failure result`() = runTest {
        val subcategoryId = "sub-1"
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId) } throws error

        val result = createRepository().fetchFlashcards(subcategoryId)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getFlashcardsBySubcategoryId(subcategoryId) }
    }
}
