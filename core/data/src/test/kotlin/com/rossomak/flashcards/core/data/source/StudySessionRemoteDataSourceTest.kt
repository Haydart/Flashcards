package com.rossomak.flashcards.core.data.source

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.rossomak.flashcards.core.domain.model.CardProgressUpdate
import com.rossomak.flashcards.core.domain.model.FlashcardResult
import com.rossomak.flashcards.core.domain.model.FlashcardStudyProgressState
import com.rossomak.flashcards.core.domain.model.ProgressSummaryWrite
import com.rossomak.flashcards.core.domain.model.SessionCommit
import com.rossomak.flashcards.core.domain.model.SessionResult
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressSummaryDelta
import com.rossomak.flashcards.core.domain.model.SubcategoryProgressWrite
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
 * Thin secondary seam: only the mapping between [SessionCommit] and the stored documents' fields,
 * in both modes, plus the fire-and-forget commit/rejection wiring. The rich behavioural rules —
 * which fields a mode carries, what gets counted, which cards a commit writes — are pinned at the
 * domain level; this test only checks they survive translation into raw Firestore documents.
 */
class StudySessionRemoteDataSourceTest {

    private val firestore: FirebaseFirestore = mockk()
    private val firebaseAuth: FirebaseAuth = mockk()
    private val firebaseUser: FirebaseUser = mockk { every { uid } returns UID }
    private val collectionReference: CollectionReference = mockk()
    private val documentReference: DocumentReference = mockk()
    private val progressDocumentReference: DocumentReference = mockk()
    private val progressSummaryDocumentReference: DocumentReference = mockk()
    private val writeBatch: WriteBatch = mockk()
    private val cardProgressRemoteDataSource: CardProgressRemoteDataSource = mockk()
    private val progressSummaryRemoteDataSource: ProgressSummaryRemoteDataSource = mockk()

    private fun createDataSource(): StudySessionRemoteDataSource =
        StudySessionRemoteDataSource(firestore, firebaseAuth, cardProgressRemoteDataSource, progressSummaryRemoteDataSource)

    @Before
    fun setUp() {
        every { firebaseAuth.currentUser } returns firebaseUser
        every { firestore.collection("users/$UID/sessions") } returns collectionReference
        every { collectionReference.document(any()) } returns documentReference
        every { firestore.batch() } returns writeBatch
        every { writeBatch.set(any(), any()) } returns writeBatch
        every { writeBatch.set(any(), any(), any<SetOptions>()) } returns writeBatch
        every { writeBatch.commit() } returns Tasks.forResult(null)
        every { cardProgressRemoteDataSource.documentReference(any()) } returns progressDocumentReference
        every { cardProgressRemoteDataSource.toMergeFields(any()) } returns MERGE_FIELDS
        every { progressSummaryRemoteDataSource.documentReference() } returns progressSummaryDocumentReference
        every { progressSummaryRemoteDataSource.toMergeFields(any()) } returns SUMMARY_MERGE_FIELDS
    }

    private fun ratedResult(): SessionResult = SessionResult.Rated(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = listOf(
            FlashcardResult.Rated(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Mastered,
                attemptsUsed = 1,
                wasPreviouslyMastered = false,
            ),
        ),
    )

    private fun fastResult(): SessionResult = SessionResult.Fast(
        id = "session-1",
        startedAt = Instant.parse("2026-09-06T10:00:00Z"),
        durationSeconds = 60,
        abandoned = false,
        categoryId = "cat-1",
        categoryName = "Category",
        subcategoryIds = listOf("sub-1"),
        subcategoryNames = listOf("Subcategory"),
        cardResults = listOf(
            FlashcardResult.Fast(
                cardId = "card-1",
                subcategoryId = "sub-1",
                state = FlashcardStudyProgressState.Seen,
            ),
        ),
    )

    private fun commit(
        sessionResult: SessionResult = ratedResult(),
        newCardsStudied: Int = 0,
        progressWrites: List<SubcategoryProgressWrite> = emptyList(),
        progressSummaryWrite: ProgressSummaryWrite = ProgressSummaryWrite(emptyMap()),
    ): SessionCommit = SessionCommit(sessionResult, newCardsStudied, progressWrites, progressSummaryWrite)

    @Test
    fun `commits to the session document keyed by session id under the user`() {
        createDataSource().commitSession(commit(), onRejected = {})

        verify(exactly = 1) { collectionReference.document("session-1") }
        verify(exactly = 1) { writeBatch.commit() }
    }

    @Test
    fun `a Rated document carries the four aggregate counts and per-entry attempts and previously-mastered`() {
        val fieldsSlot = slot<Map<String, Any>>()
        every { writeBatch.set(documentReference, capture(fieldsSlot)) } returns writeBatch

        createDataSource().commitSession(commit(ratedResult(), newCardsStudied = 1), onRejected = {})

        val fields = fieldsSlot.captured
        fields["cardsMastered"] shouldBe 1
        fields["cardsPartial"] shouldBe 0
        fields["cardsDefended"] shouldBe 0
        fields["cardsDemastered"] shouldBe 0
        fields["newCardsStudied"] shouldBe 1
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

        createDataSource().commitSession(commit(fastResult()), onRejected = {})

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
    fun `each progress write joins the same batch as a merge set keyed by its subcategory id`() {
        val write = SubcategoryProgressWrite(
            subcategoryId = "sub-1",
            categoryId = "cat-1",
            cards = mapOf("card-1" to CardProgressUpdate(FlashcardStudyProgressState.Mastered, stampFirstStudied = true, stampMastered = true)),
        )

        createDataSource().commitSession(commit(progressWrites = listOf(write)), onRejected = {})

        verify(exactly = 1) { cardProgressRemoteDataSource.documentReference("sub-1") }
        verify(exactly = 1) { cardProgressRemoteDataSource.toMergeFields(write) }
        verify(exactly = 1) { writeBatch.set(progressDocumentReference, MERGE_FIELDS, any<SetOptions>()) }
        verify(exactly = 1) { writeBatch.commit() }
    }

    @Test
    fun `a session with no progress writes adds no merge set to the batch`() {
        createDataSource().commitSession(commit(progressWrites = emptyList()), onRejected = {})

        verify(exactly = 0) { writeBatch.set(any(), any(), any<SetOptions>()) }
    }

    @Test
    fun `a non-empty progress-summary write joins the same batch as a merge set at the fixed document id`() {
        val write = ProgressSummaryWrite(mapOf("sub-1" to SubcategoryProgressSummaryDelta(masteredDelta = 1, studiedDelta = 1)))

        createDataSource().commitSession(commit(progressSummaryWrite = write), onRejected = {})

        verify(exactly = 1) { progressSummaryRemoteDataSource.documentReference() }
        verify(exactly = 1) { progressSummaryRemoteDataSource.toMergeFields(write) }
        verify(exactly = 1) { writeBatch.set(progressSummaryDocumentReference, SUMMARY_MERGE_FIELDS, any<SetOptions>()) }
        verify(exactly = 1) { writeBatch.commit() }
    }

    @Test
    fun `a session with no progress-summary deltas adds no summary merge set to the batch`() {
        createDataSource().commitSession(commit(progressSummaryWrite = ProgressSummaryWrite(emptyMap())), onRejected = {})

        verify(exactly = 0) { progressSummaryRemoteDataSource.documentReference() }
    }

    @Test
    fun `a rejected commit invokes onRejected with the exception`() {
        val error = Exception("permission denied")
        every { writeBatch.commit() } returns Tasks.forException(error)
        var reported: Throwable? = null

        createDataSource().commitSession(commit()) { rejection -> reported = rejection }

        reported shouldBe error
    }

    @Test
    fun `a not-yet-resolved (queued offline) commit never invokes onRejected`() {
        val pendingTask: Task<Void> = TaskCompletionSource<Void>().task
        every { writeBatch.commit() } returns pendingTask
        var reported: Throwable? = null

        createDataSource().commitSession(commit()) { rejection -> reported = rejection }

        reported shouldBe null
    }

    private companion object {
        const val UID = "user-1"
        val MERGE_FIELDS: Map<String, Any> = mapOf("categoryId" to "cat-1", "cards" to mapOf("card-1" to mapOf("state" to "Mastered")))
        val SUMMARY_MERGE_FIELDS: Map<String, Any> = mapOf("subcategories" to mapOf("sub-1" to mapOf("masteredCount" to 1, "studiedCount" to 1)))
    }
}
