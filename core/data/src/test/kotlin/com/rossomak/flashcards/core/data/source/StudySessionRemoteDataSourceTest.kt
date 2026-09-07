package com.rossomak.flashcards.core.data.source

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.SessionLedgerEntry
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.StudyMode
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import org.junit.Before
import org.junit.Test

/**
 * Thin secondary seam: only the mapping between [SessionResult] and the stored document's fields,
 * in both modes, plus the fire-and-forget commit/rejection wiring. The rich behavioural rules —
 * which fields a mode carries, what gets counted — are pinned at the domain level; this test only
 * checks they survive translation into a raw Firestore document.
 */
class StudySessionRemoteDataSourceTest {

    private val firestore: FirebaseFirestore = mockk()
    private val firebaseAuth: FirebaseAuth = mockk()
    private val firebaseUser: FirebaseUser = mockk { every { uid } returns UID }
    private val collectionReference: CollectionReference = mockk()
    private val documentReference: DocumentReference = mockk()
    private val writeBatch: WriteBatch = mockk()

    private fun createDataSource(): StudySessionRemoteDataSource = StudySessionRemoteDataSource(firestore, firebaseAuth)

    @Before
    fun setUp() {
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firestore.collection("users/$UID/sessions") } returns collectionReference
        every { collectionReference.document(any()) } returns documentReference
        every { firestore.batch() } returns writeBatch
        every { writeBatch.set(any(), any()) } returns writeBatch
        every { writeBatch.commit() } returns Tasks.forResult(null)
    }

    private fun ratedResult(): SessionResult = SessionResult(
        id = "session-1",
        mode = StudyMode.Rated,
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        ledger = listOf(
            SessionLedgerEntry(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        ),
    )

    private fun fastResult(): SessionResult = ratedResult().copy(
        mode = StudyMode.Fast,
        ledger = listOf(
            SessionLedgerEntry(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Seen,
                attemptsUsed = 0,
                wasPreviouslyMastered = false,
            ),
        ),
    )

    @Test
    fun `commits to the session document keyed by session id under the user`() {
        createDataSource().commitSession(ratedResult(), onRejected = {})

        verify(exactly = 1) { collectionReference.document("session-1") }
        verify(exactly = 1) { writeBatch.commit() }
    }

    @Test
    fun `a Rated document carries the four aggregate counts and per-entry attempts and previously-mastered`() {
        val fieldsSlot = slot<Map<String, Any>>()
        every { writeBatch.set(documentReference, capture(fieldsSlot)) } returns writeBatch

        createDataSource().commitSession(ratedResult(), onRejected = {})

        val fields = fieldsSlot.captured
        fields["cardsMastered"] shouldBe 1
        fields["cardsPartial"] shouldBe 0
        fields["cardsDefended"] shouldBe 0
        fields["cardsDemastered"] shouldBe 0
        fields["newCardsStudied"] shouldBe 0
        @Suppress("UNCHECKED_CAST")
        val cardResults = fields["cardResults"] as Map<String, Map<String, Any>>
        val entry = cardResults.getValue("card-1")
        entry["state"] shouldBe "Mastered"
        entry["attemptsUsed"] shouldBe 1
        entry["wasPreviouslyMastered"] shouldBe false
    }

    @Test
    fun `a Fast document carries none of the four Rated-only aggregate fields`() {
        val fieldsSlot = slot<Map<String, Any>>()
        every { writeBatch.set(documentReference, capture(fieldsSlot)) } returns writeBatch

        createDataSource().commitSession(fastResult(), onRejected = {})

        val fields = fieldsSlot.captured
        fields.keys shouldNotContain "cardsMastered"
        fields.keys shouldNotContain "cardsPartial"
        fields.keys shouldNotContain "cardsDefended"
        fields.keys shouldNotContain "cardsDemastered"
        @Suppress("UNCHECKED_CAST")
        val cardResults = fields["cardResults"] as Map<String, Map<String, Any>>
        val entry = cardResults.getValue("card-1")
        entry["state"] shouldBe "Seen"
        entry.keys shouldNotContain "attemptsUsed"
        entry.keys shouldNotContain "wasPreviouslyMastered"
    }

    @Test
    fun `a rejected commit invokes onRejected with the exception`() {
        val error = Exception("permission denied")
        every { writeBatch.commit() } returns Tasks.forException(error)
        var reported: Throwable? = null

        createDataSource().commitSession(ratedResult()) { rejection -> reported = rejection }

        reported shouldBe error
    }

    @Test
    fun `a not-yet-resolved (queued offline) commit never invokes onRejected`() {
        val pendingTask: Task<Void> = TaskCompletionSource<Void>().task
        every { writeBatch.commit() } returns pendingTask
        var reported: Throwable? = null

        createDataSource().commitSession(ratedResult()) { rejection -> reported = rejection }

        reported shouldBe null
    }

    private companion object {
        const val UID = "user-1"
    }
}
