package com.rossomak.flashcards.core.data.repository

import com.google.firebase.Timestamp
import com.rossomak.flashcards.core.data.model.CurationActionEntryDto
import com.rossomak.flashcards.core.data.model.CurationRequestDto
import com.rossomak.flashcards.core.data.source.CurationRemoteDataSource
import com.rossomak.flashcards.core.domain.model.CurationAction
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCurationRepositoryTest {

    private val remoteDataSource: CurationRemoteDataSource = mockk()

    private fun createRepository(): DefaultCurationRepository =
        DefaultCurationRepository(remoteDataSource)

    @Test
    fun `getCurationRequests maps dtos to domain keyed by card id`() = runTest {
        val cardId = "card-1"
        val cardIds = listOf(cardId)
        val flaggedAt = Timestamp(Date(1_000L))
        val dto = CurationRequestDto(
            subcategoryId = "sub-1",
            actions = mapOf(CurationAction.Delete.name to CurationActionEntryDto(flaggedAt = flaggedAt)),
        )
        coEvery { remoteDataSource.getCurationRequests(cardIds) } returns mapOf(cardId to dto)

        val result = createRepository().getCurationRequests(cardIds)

        result.isSuccess shouldBe true
        val request = result.getOrThrow().getValue(cardId)
        request.cardId shouldBe cardId
        request.subcategoryId shouldBe dto.subcategoryId
        request.actions.keys shouldBe setOf(CurationAction.Delete)
        coVerify(exactly = 1) { remoteDataSource.getCurationRequests(cardIds) }
    }

    @Test
    fun `getCurationRequests wraps data source failure in failure result`() = runTest {
        val cardIds = listOf("card-1")
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.getCurationRequests(cardIds) } throws error

        val result = createRepository().getCurationRequests(cardIds)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.getCurationRequests(cardIds) }
    }

    @Test
    fun `getCurationRequests rethrows cancellation instead of wrapping it`() = runTest {
        val cardIds = listOf("card-1")
        coEvery { remoteDataSource.getCurationRequests(cardIds) } throws CancellationException("cancelled")

        val thrown = runCatching { createRepository().getCurationRequests(cardIds) }.exceptionOrNull()

        (thrown is CancellationException) shouldBe true
        coVerify(exactly = 1) { remoteDataSource.getCurationRequests(cardIds) }
    }

    @Test
    fun `upsertCurationActions forwards identifiers and the whole action set to data source`() = runTest {
        val cardId = "card-1"
        val subcategoryId = "sub-1"
        val actions = setOf(CurationAction.DifficultyTooHard, CurationAction.WrongTags)
        coEvery { remoteDataSource.upsertCurationActions(cardId, subcategoryId, actions) } just Runs

        val result = createRepository().upsertCurationActions(cardId, subcategoryId, actions)

        result.isSuccess shouldBe true
        coVerify(exactly = 1) { remoteDataSource.upsertCurationActions(cardId, subcategoryId, actions) }
    }

    @Test
    fun `upsertCurationActions wraps data source failure in failure result`() = runTest {
        val cardId = "card-1"
        val subcategoryId = "sub-1"
        val actions = setOf(CurationAction.DifficultyTooHard)
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.upsertCurationActions(cardId, subcategoryId, actions) } throws error

        val result = createRepository().upsertCurationActions(cardId, subcategoryId, actions)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.upsertCurationActions(cardId, subcategoryId, actions) }
    }

    @Test
    fun `removeCurationAction forwards card id and action to data source`() = runTest {
        val cardId = "card-1"
        val action = CurationAction.Delete
        coEvery { remoteDataSource.removeCurationAction(cardId, action) } just Runs

        val result = createRepository().removeCurationAction(cardId, action)

        result.isSuccess shouldBe true
        coVerify(exactly = 1) { remoteDataSource.removeCurationAction(cardId, action) }
    }

    @Test
    fun `removeCurationAction wraps data source failure in failure result`() = runTest {
        val cardId = "card-1"
        val action = CurationAction.Delete
        val error = IllegalStateException("firestore down")
        coEvery { remoteDataSource.removeCurationAction(cardId, action) } throws error

        val result = createRepository().removeCurationAction(cardId, action)

        result.isFailure shouldBe true
        result.exceptionOrNull() shouldBe error
        coVerify(exactly = 1) { remoteDataSource.removeCurationAction(cardId, action) }
    }
}
