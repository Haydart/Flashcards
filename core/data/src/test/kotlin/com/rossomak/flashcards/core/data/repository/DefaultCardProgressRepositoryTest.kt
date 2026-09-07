package com.rossomak.flashcards.core.data.repository

import com.google.firebase.Timestamp
import com.rossomak.flashcards.core.data.model.CardProgressEntryDto
import com.rossomak.flashcards.core.data.model.ProgressSummaryDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressDto
import com.rossomak.flashcards.core.data.model.SubcategoryProgressSummaryDto
import com.rossomak.flashcards.core.data.source.CardProgressRemoteDataSource
import com.rossomak.flashcards.core.data.source.ProgressSummaryRemoteDataSource
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCardProgressRepositoryTest {

    private val remoteDataSource: CardProgressRemoteDataSource = mockk()
    private val progressSummaryRemoteDataSource: ProgressSummaryRemoteDataSource = mockk()

    private fun createRepository(): DefaultCardProgressRepository =
        DefaultCardProgressRepository(remoteDataSource, progressSummaryRemoteDataSource)

    @Test
    fun `getProgress maps the dto to domain keyed by the requested subcategory id`() = runTest {
        val subcategoryId = "sub-1"
        val dto = SubcategoryProgressDto(
            categoryId = "cat-1",
            cards = mapOf(
                "card-1" to CardProgressEntryDto(
                    state = FlashcardStudyProgressState.Mastered.name,
                    firstStudiedAt = Timestamp(Date(1_000L)),
                ),
            ),
        )
        coEvery { remoteDataSource.getProgress(subcategoryId) } returns dto

        val result = createRepository().getProgress(subcategoryId)

        result.isSuccess shouldBe true
        val progress = result.getOrThrow()
        progress?.subcategoryId shouldBe subcategoryId
        progress?.categoryId shouldBe "cat-1"
        progress?.cards?.keys shouldBe setOf("card-1")
        coVerify(exactly = 1) { remoteDataSource.getProgress(subcategoryId) }
    }

    @Test
    fun `getProgress returns success with null for an absent document`() = runTest {
        val subcategoryId = "sub-1"
        coEvery { remoteDataSource.getProgress(subcategoryId) } returns null

        val result = createRepository().getProgress(subcategoryId)

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe null
        coVerify(exactly = 1) { remoteDataSource.getProgress(subcategoryId) }
    }

    @Test
    fun `getProgress wraps a data source failure in a failure result`() = runTest {
        val subcategoryId = "sub-1"
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getProgress(subcategoryId) } throws error

        val result = createRepository().getProgress(subcategoryId)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getProgress(subcategoryId) }
    }

    @Test
    fun `getProgress rethrows cancellation instead of wrapping it`() = runTest {
        val subcategoryId = "sub-1"
        coEvery { remoteDataSource.getProgress(subcategoryId) } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().getProgress(subcategoryId) }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { remoteDataSource.getProgress(subcategoryId) }
    }

    @Test
    fun `getProgressSummary maps the dto to domain keyed by subcategory id`() = runTest {
        val subcategoryId = "sub-1"
        val masteredCount = 3
        val studiedCount = 5
        val dto = ProgressSummaryDto(
            subcategories = mapOf(subcategoryId to SubcategoryProgressSummaryDto(masteredCount = masteredCount, studiedCount = studiedCount)),
        )
        coEvery { progressSummaryRemoteDataSource.getSummary() } returns dto

        val result = createRepository().getProgressSummary()

        result.isSuccess shouldBe true
        val subcategory = result.getOrThrow()?.subcategories?.getValue(subcategoryId)
        subcategory?.masteredCount shouldBe masteredCount
        subcategory?.studiedCount shouldBe studiedCount
        coVerify(exactly = 1) { progressSummaryRemoteDataSource.getSummary() }
    }

    @Test
    fun `getProgressSummary returns success with null for an absent document`() = runTest {
        coEvery { progressSummaryRemoteDataSource.getSummary() } returns null

        val result = createRepository().getProgressSummary()

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe null
        coVerify(exactly = 1) { progressSummaryRemoteDataSource.getSummary() }
    }

    @Test
    fun `getProgressSummary wraps a data source failure in a failure result`() = runTest {
        val error = IllegalStateException("firestore down")
        coEvery { progressSummaryRemoteDataSource.getSummary() } throws error

        val result = createRepository().getProgressSummary()

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { progressSummaryRemoteDataSource.getSummary() }
    }

    @Test
    fun `getProgressSummary rethrows cancellation instead of wrapping it`() = runTest {
        coEvery { progressSummaryRemoteDataSource.getSummary() } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().getProgressSummary() }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { progressSummaryRemoteDataSource.getSummary() }
    }
}
