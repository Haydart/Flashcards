package com.rossomak.flashcards.feature.study.rated

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.FlashcardRating
import com.rossomak.flashcards.core.domain.model.RatedSessionState
import com.rossomak.flashcards.core.domain.model.UserPreference.VoiceAnswerConsent as VoiceAnswerConsentPreference
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings as SavedVoiceSettings
import com.rossomak.flashcards.core.domain.model.rate
import com.rossomak.flashcards.core.domain.model.requeueAfterSilence
import com.rossomak.flashcards.core.domain.model.toFlashcardRating
import com.rossomak.flashcards.core.domain.usecase.GetFlashcardsUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveUserPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveUserPreferenceUseCase
import com.rossomak.flashcards.core.domain.usecase.SubmitCurationReportUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.toVoiceSettings
import com.rossomak.flashcards.feature.study.RatedStudySessionRoute
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ExitSession
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ExtendedContext
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.ReportProblem
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.VoiceAnswerConsent
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialog.VoiceSettings
import com.rossomak.flashcards.feature.study.chrome.StudySessionDialogEvent
import com.rossomak.flashcards.feature.study.voice.VoiceAnswerPhase
import com.rossomak.flashcards.feature.study.voice.VoiceGateway
import com.rossomak.flashcards.feature.study.voice.VoicePhase
import com.rossomak.flashcards.feature.study.voice.VoicePlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs a Rated Study Session end to end — reveal, the Failed/Partial/Correct row, and the
 * in-session voice-answering toggle with its consent and microphone flow (ticket 03 of
 * [ADR-0045](../../../../../../../../docs/adr/0045-separate-fast-and-rated-session-screens.md)).
 * Knows nothing about Read-aloud or auto-start playback — those are Fast concepts.
 *
 * The user-preferences use cases live here and only here: their sole current purpose is the
 * voice-answering consent flag (ADR-0025), and Fast has no path to it.
 *
 * The rating callback drives a [RatedSessionState]: a Correct rating finishes a card as Mastered,
 * Failed/Partial re-insert it further down the queue (or finish it, per
 * [RatedStudySessionRoute.partialRatingCardRequeueingEnabled] and the Attempts limit), and the
 * session's terminal navigation event fires once the queue empties (ticket 02 of the Rated session
 * state machine sequence). A voice grade drives the exact same [onRating] path as a manual tap; a
 * silence timeout instead consumes no Attempt, and three in a row pause the session rather than
 * finishing it (ticket 04).
 */
@HiltViewModel
class RatedStudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFlashcards: GetFlashcardsUseCase,
    private val submitCurationReport: SubmitCurationReportUseCase,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val saveUserPreference: SaveUserPreferenceUseCase,
    private val voiceGateway: VoiceGateway,
    private val voiceSettingsController: VoiceSettingsController,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<RatedStudySessionRoute>()
    private val sessionTitle: String = route.sessionTitle

    private val _state = MutableStateFlow(
        RatedStudySessionScreenState(sessionTitle = sessionTitle, attemptsLimit = route.ratedAttempts),
    )
    val state: StateFlow<RatedStudySessionScreenState> = _state.asStateFlow()

    // Tracks eagerly so rapid toggles don't race against isVoiceActive propagation.
    private var voiceStarted = false

    internal var rewindThresholdMs: Long = VoicePlaybackState.REWIND_THRESHOLD_MS

    // Test-only seam for asserting a deterministic queue sequence (ADR-0046) — production leaves
    // this as Random.Default and never seeds it.
    internal var random: Random = Random.Default

    private var rewindJob: Job? = null
    private var isPastRewindThreshold = false
    private val eventChannel = Channel<RatedStudySessionDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var lastObservedCardIndex = -1

    // Seeded once the routed cards resolve (loadFlashcards); null only during that initial load.
    private var ratedSessionState: RatedSessionState? = null

    // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge): plain android.util.Log
    // is unmocked in this module's JVM unit tests (no Robolectric), so every call site below routes
    // through this instead of Log.d directly — swallows the "not mocked" RuntimeException there,
    // logs for real on device.
    private fun logCr(message: String) {
        runCatching { Log.d(TEST_LOG_TAG, message) }
    }

    // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge): compact partial
    // state dump appended to most operation logs below, so a single logcat line shows what changed
    // and what the rest of the session looked like at that instant.
    private fun stateSnapshot(): String {
        val s = _state.value
        return "card=${s.currentCard?.id} idx=${s.currentCardIndex} mastered=${s.masteredCount}/${s.distinctCardCount} " +
            "ratings=${s.currentCardRatings} remaining=${s.flashcards.size} silenceCount=$consecutiveSilenceCount " +
            "voice(active=${s.isVoiceActive} playing=${s.isVoicePlaying} answerPhase=${s.voiceAnswerPhase} paused=${s.isVoiceAnswerPaused})"
    }

    private val isExtendedContextDialogOpen: Boolean
        get() = _state.value.activeDialog is ExtendedContext

    // True only when the pause was caused by the dialog intercepting a natural between-card advance.
    // Gates auto-advance on dialog dismiss and changes play-button behavior.
    private var pausedDueToExtendedContext = false
    private var advanceAfterExtendedContextJob: Job? = null

    // True only when opening voice settings paused an in-progress playback; gates resume on close.
    private var pausedForVoiceSettings = false

    private var hasVoiceAnswerConsent = false

    // Edge-detects a fresh arrival at SpeakingNotice in observeVoiceAnswerState — the collector
    // sees every VoiceAnswerState the gateway emits, but a grade/silence-timeout must apply exactly
    // once per round, not once per equal-value re-collection.
    private var previousVoiceAnswerPhase = VoiceAnswerPhase.Idle

    // Holds the screen-visible half of a voice-graded rating or silence-timeout (queue reseed,
    // currentCard/currentCardRatings, answer-reveal reset, terminal navigation) while the grade or
    // skip notice is still being spoken. The queue reducer itself (ratedSessionState) still updates
    // immediately — only what the user sees is held back — so the top of the screen keeps showing
    // the card the feedback is actually about instead of jumping to the next question mid-notice.
    // Runs the moment the phase leaves SpeakingNotice (see observeVoiceAnswerState), whatever the
    // reason (notice finished naturally, or voice answering was torn down mid-notice).
    private var pendingSessionSync: (() -> Unit)? = null

    // Session-scoped, not per-card (ticket 04): counts consecutive silence timeouts, reset by any
    // graded answer, and pauses the session on reaching CONSECUTIVE_SILENCE_PAUSE_THRESHOLD.
    private var consecutiveSilenceCount = 0

    // Session-scoped like the rest of the routed config: a mid-session change updates only this
    // running session unless the user checks "keep as my default" (ADR-0030), so it lives in a
    // plain var rather than being re-read from the controller on every playback start.
    private var sessionVoiceSettings: SavedVoiceSettings = route.voiceSettings

    init {
        loadFlashcards()
        observeVoiceState()
        observeVoiceAnswerState()
        observeVoiceAnswerConsentState()
    }

    // Card selection happens on the Preview Study Session screen (ADR-0004); the session only
    // resolves the routed cardIds to full Flashcards, preserving the routed order.
    private fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val results = coroutineScope {
                route.subcategoryIds
                    .map { subcategoryId -> async { getFlashcards(subcategoryId) } }
                    .awaitAll()
            }
            if (results.any { it.isFailure }) {
                _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                return@launch
            }
            val cardsById = results.flatMap { it.getOrThrow() }.associateBy { it.id }
            val sessionCards = route.cardIds.mapNotNull(cardsById::get)
            ratedSessionState = RatedSessionState.seed(
                cards = sessionCards,
                attemptsLimit = route.ratedAttempts,
                partialRatingCardRequeueingEnabled = route.partialRatingCardRequeueingEnabled,
                random = random,
            )
            _state.update { it.copy(isLoading = false) }
            syncStateFromRatedSession()
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
            logCr("loadFlashcards done, ${sessionCards.size} cards | ${stateSnapshot()}")
            honourRoutedVoiceAnswering(hasCards = sessionCards.isNotEmpty())
        }
    }

    /**
     * Mirrors the machine's queue into screen state. [RatedStudySessionScreenState.currentCardIndex]
     * always lands on 0 in this path — the current card is always the queue's head.
     *
     * Also re-seeds the voice engine's queue whenever voice is active: [VoiceGateway.updateQueue]
     * swaps in [RatedSessionState.remainingCards] without touching the in-flight utterance, keeping
     * the spoken card, displayed card, and reducer head from diverging once a rating or silence
     * timeout reorders the queue (ADR-0046).
     */
    private fun syncStateFromRatedSession() {
        val machine = ratedSessionState ?: return
        _state.update {
            it.copy(
                flashcards = machine.remainingCards,
                currentCardIndex = 0,
                masteredCount = machine.masteredCount,
                distinctCardCount = machine.distinctCardCount,
                currentCardRatings = machine.currentCardRatings,
            )
        }
        // voiceStarted, not just isVoiceActive: a rating can land after voiceGateway.start() was
        // called but before the async bind actually completes (isVoiceActive still false at that
        // point) — StudySessionVoiceGateway.updateQueue() unconditionally updates its pendingCards
        // regardless of bind state, so this still reaches the gateway before onServiceConnected()
        // loads it, rather than leaving it to load the stale pre-rating order.
        if (_state.value.isVoiceActive || voiceStarted) {
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge): confirms
            // the voice gateway is actually re-seeded on every reorder, and with what order.
            logCr("updateQueue -> ${machine.remainingCards.map { it.id }}")
            voiceGateway.updateQueue(machine.remainingCards)
        }
    }

    /**
     * The Preview screen's voice-answering choice (ADR-0030) takes effect on entry, running the
     * same consent-then-microphone path the in-session toggle uses.
     *
     * Consent is read as a one-shot rather than from [hasVoiceAnswerConsent], whose collector may
     * not have emitted yet by the time the cards land.
     */
    private suspend fun honourRoutedVoiceAnswering(hasCards: Boolean) {
        if (!route.voiceAnsweringEnabled || !hasCards) return
        requestVoiceAnswering(observeUserPreferences().first().voiceAnswerConsentGranted)
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            voiceGateway.state.collect { voice ->
                if (voice.error != null) {
                    // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
                    logCr("observeVoiceState error=${voice.error} | ${stateSnapshot()}")
                    voiceStarted = false
                    _state.update { it.copy(isVoiceActive = false, isVoicePlaying = false, voiceError = voice.error) }
                    return@collect
                }
                _state.update {
                    it.copy(
                        isVoiceActive = voice.isActive,
                        isVoicePlaying = voice.isPlaying,
                        speechRate = voice.speechRate,
                        currentCardIndex = if (voice.isActive) voice.currentIndex else it.currentCardIndex,
                        // Grading/feedback also reveals the card (see observeVoiceAnswerState) —
                        // don't let this collector's phase check stomp that back to false while
                        // the TTS engine itself is still sitting on QUESTION.
                        isAnswerRevealed = if (voice.isActive) {
                            voice.phase == VoicePhase.Answer ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.Grading ||
                                it.voiceAnswerPhase == VoiceAnswerPhase.SpeakingNotice
                        } else {
                            it.isAnswerRevealed
                        },
                    )
                }
                if (voice.isActive && voice.currentIndex != lastObservedCardIndex) {
                    lastObservedCardIndex = voice.currentIndex
                    advanceAfterExtendedContextJob?.cancel()
                    pausedDueToExtendedContext = false
                    startRewindThresholdTimer()
                } else if (!voice.isActive) {
                    voiceStarted = false
                    lastObservedCardIndex = -1
                    advanceAfterExtendedContextJob?.cancel()
                    pausedDueToExtendedContext = false
                    rewindJob?.cancel()
                    isPastRewindThreshold = false
                }
                if (voice.isInBetweenPause && voice.isPlaying && isExtendedContextDialogOpen && !pausedDueToExtendedContext) {
                    pausedDueToExtendedContext = true
                    viewModelScope.launch { voiceGateway.togglePlayPause() }
                }
            }
        }
    }

    private fun observeVoiceAnswerState() {
        viewModelScope.launch {
            voiceGateway.voiceAnswerState.collect { voiceAnswer ->
                // Edge-detected before the state update below, off the collector's own running
                // previousVoiceAnswerPhase — SpeakingNotice is entered exactly once per graded or
                // silence-timed-out round, never re-triggered by an equal-value re-collection.
                val justEnteredSpeakingNotice = voiceAnswer.phase == VoiceAnswerPhase.SpeakingNotice &&
                    previousVoiceAnswerPhase != VoiceAnswerPhase.SpeakingNotice
                // Mirrors justEnteredSpeakingNotice the other way: fires exactly once, the instant
                // the grade/skip notice stops being the active phase — whether that's the natural
                // WaitingForQuestion it flips to once the notice finishes speaking, or voice
                // answering getting torn down mid-notice. Either way the deferred sync below is safe
                // to run: it's idempotent and there is nothing left mid-notice to interrupt.
                val justLeftSpeakingNotice = previousVoiceAnswerPhase == VoiceAnswerPhase.SpeakingNotice &&
                    voiceAnswer.phase != VoiceAnswerPhase.SpeakingNotice
                previousVoiceAnswerPhase = voiceAnswer.phase
                _state.update {
                    it.copy(
                        isVoiceAnswerEnabled = voiceAnswer.isEnabled,
                        voiceAnswerPhase = voiceAnswer.phase,
                        voiceAnswerSanitizedTranscript = voiceAnswer.sanitizedTranscript,
                        lastVoiceAnswerGrade = voiceAnswer.lastGrade,
                        voiceAnswerError = voiceAnswer.error,
                        // Grading starts as soon as the utterance is captured, before the TTS
                        // engine's own phase would flip to ANSWER — reveal the card now so the
                        // user can check what they missed while grading/feedback plays out.
                        isAnswerRevealed = it.isAnswerRevealed ||
                            voiceAnswer.phase == VoiceAnswerPhase.Grading,
                    )
                }
                if (justLeftSpeakingNotice) {
                    pendingSessionSync?.invoke()
                    pendingSessionSync = null
                }
                if (!justEnteredSpeakingNotice) return@collect
                // ADR-0026: lastGrade == null distinguishes a silence-timeout skip from a real
                // graded result — both share SpeakingNotice, never a dedicated phase value. But a
                // grading/transcription failure also lands in SpeakingNotice with lastGrade == null
                // (VoiceAnswerController's catch block never sets a grade), so error must be ruled
                // out first or a backend failure gets silently counted as silence.
                val grade = voiceAnswer.lastGrade
                when {
                    grade != null -> onVoiceGraded(grade, voiceAnswer.lastGradedCardId)
                    voiceAnswer.error != null -> {
                        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before
                        // merge): confirms a grading/transcription failure is no longer counted
                        // as a silence timeout (was: bumped consecutiveSilenceCount, could
                        // trigger the repeated-silence pause after 3 unrelated network errors).
                        logCr("grading error ignored, not counted as silence: ${voiceAnswer.error}")
                    }
                    else -> onVoiceSilenceTimeout()
                }
            }
        }
    }

    /**
     * The one path a voice grade applies a Rating through — [onRating] itself, exactly like a
     * manual tap, using the fixed grade-band mapping (ticket 04 of the Rated session state machine
     * sequence). An actual graded utterance is the only proof someone is there, so this is also the
     * one place [consecutiveSilenceCount] resets.
     */
    /**
     * [gradedCardId] guards against grading a card the reducer head has already moved past — the
     * Rated voice transport still allows Next while a question is being read (before listening
     * opens), so a grade can in principle land for a card that isn't the current head any more. A
     * mismatch means this grade is stale; drop it rather than rating whatever the head currently is.
     */
    private fun onVoiceGraded(grade: VoiceAnswerGrade, gradedCardId: String?) {
        val headCardId = ratedSessionState?.currentCard?.id
        if (gradedCardId != null && gradedCardId != headCardId) {
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
            logCr("onVoiceGraded ignored, gradedCardId=$gradedCardId != head=$headCardId")
            return
        }
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoiceGraded percent=${grade.gradePercent} -> ${grade.toFlashcardRating()} | ${stateSnapshot()}")
        consecutiveSilenceCount = 0
        applyRating(grade.toFlashcardRating(), deferSync = true)
    }

    /**
     * A silence timeout: no Attempt, no Rating — the card is put back unchanged, using the Failed
     * gap range. Three in a row pauses the session rather than letting an unattended phone cycle
     * the deck indefinitely.
     *
     * The reducer updates right away, but what the screen shows waits like [applyRating]'s deferred
     * path does — the "didn't hear you" notice is about the still-displayed card, so the queue's
     * next head must not appear until that notice finishes.
     */
    private fun onVoiceSilenceTimeout() {
        val cardBefore = ratedSessionState?.currentCard?.id
        ratedSessionState = ratedSessionState?.let(::requeueAfterSilence)
        consecutiveSilenceCount++
        pendingSessionSync = {
            syncStateFromRatedSession()
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
            logCr("onVoiceSilenceTimeout card=$cardBefore requeued | ${stateSnapshot()}")
        }
        if (consecutiveSilenceCount >= CONSECUTIVE_SILENCE_PAUSE_THRESHOLD) {
            pauseForRepeatedSilence()
        }
    }

    /**
     * Pausing is not ending: no Terminal State, no navigation event, the queue untouched. Playback
     * and the microphone stop; only the resume affordance stays live.
     */
    private fun pauseForRepeatedSilence() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("pauseForRepeatedSilence | ${stateSnapshot()}")
        if (_state.value.isVoicePlaying) voiceGateway.togglePlayPause()
        voiceGateway.setVoiceAnswering(false)
        _state.update { it.copy(isVoiceAnswerPaused = true) }
    }

    /** Re-arms voice answering on the same card, counter back at zero. */
    fun onResumeSession() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onResumeSession | ${stateSnapshot()}")
        consecutiveSilenceCount = 0
        _state.update { it.copy(isVoiceAnswerPaused = false) }
        voiceGateway.setVoiceAnswering(true)
        if (!_state.value.isVoicePlaying) voiceGateway.togglePlayPause()
    }

    private fun observeVoiceAnswerConsentState() {
        viewModelScope.launch {
            observeUserPreferences().map { it.voiceAnswerConsentGranted }.collect { hasConsent ->
                hasVoiceAnswerConsent = hasConsent
            }
        }
    }

    fun onVoiceAnswerToggle() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoiceAnswerToggle enabled=${_state.value.isVoiceAnswerEnabled} | ${stateSnapshot()}")
        if (_state.value.isVoiceAnswerEnabled) {
            // Voice-answering-on drives the shared TTS engine in a stop-after-question shape;
            // there is no meaningful "keep reading, just stop grading" middle state (ADR-0025),
            // so disabling it tears down the whole engine back to manual Show Answer/Next.
            voiceGateway.stop()
            return
        }
        requestVoiceAnswering(hasVoiceAnswerConsent)
    }

    /** Consent first, then the microphone. Both gates are one-time; neither is skippable. */
    private fun requestVoiceAnswering(hasConsent: Boolean) {
        if (hasConsent) {
            _state.update { it.copy(isMicPermissionRequestPending = true) }
        } else {
            _state.update { it.copy(activeDialog = VoiceAnswerConsent) }
        }
    }

    private fun onVoiceAnswerConsentAccept() {
        viewModelScope.launch {
            saveUserPreference(VoiceAnswerConsentPreference(true))
                .onSuccess {
                    _state.update {
                        it.copy(
                            activeDialog = null,
                            isMicPermissionRequestPending = true,
                        )
                    }
                }
                .onFailure {
                    // Consent wasn't actually recorded — leave the dialog up rather than starting
                    // the mic as if it had been, so a retry is a single tap on the same dialog.
                    _state.update { it.copy(voiceError = "Failed to save voice answering consent") }
                }
        }
    }

    fun onMicPermissionResult(isGranted: Boolean) {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onMicPermissionResult granted=$isGranted | ${stateSnapshot()}")
        _state.update { it.copy(isMicPermissionRequestPending = false) }
        if (!isGranted) return
        // Rated sessions never auto-start the gateway; enabling voice answering is what
        // bootstraps it here (ADR-0025).
        ensureVoiceGatewayStarted()
        voiceGateway.setVoiceAnswering(true)
    }

    fun onVoiceAnswerGradeDismissed() {
        _state.update { it.copy(lastVoiceAnswerGrade = null) }
    }

    fun onShowAnswer() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onShowAnswer | ${stateSnapshot()}")
        if (_state.value.isVoiceActive) {
            voiceGateway.showAnswer()
        } else {
            _state.update { it.copy(isAnswerRevealed = true) }
        }
    }

    /**
     * Applies [rating] to the machine's current (head) card: Correct finishes it Mastered
     * immediately, Failed/Partial either re-insert it further down the queue or finish it, per the
     * Attempts limit and [RatedStudySessionRoute.partialRatingCardRequeueingEnabled]. The session
     * completes — and the terminal navigation event fires — exactly when the queue empties.
     */
    fun onRating(rating: FlashcardRating) = applyRating(rating, deferSync = false)

    /**
     * [deferSync] is what separates a manual tap from a voice grade: a tap has no feedback playing
     * over it, so the queue advance is immediate exactly like before. A voice grade instead lands
     * mid-[VoiceAnswerPhase.SpeakingNotice] — the feedback about to be read is about the card still
     * on screen, so the queue reducer updates now (the [VoiceGateway] still needs the reordered
     * queue reseeded to know what's next once the notice ends) but everything the user actually
     * sees — [RatedStudySessionScreenState.currentCard]/`currentCardRatings`, the answer-reveal
     * reset, and the terminal navigation event — is captured into [pendingSessionSync] and only
     * runs once that notice actually finishes (observeVoiceAnswerState's SpeakingNotice-exit edge).
     */
    private fun applyRating(rating: FlashcardRating, deferSync: Boolean) {
        val machine = ratedSessionState ?: return
        // A rapid second tap, or a late voice grade/silence timeout racing the terminal navigation
        // event, can still reach here after the queue has emptied — rate() assumes a head to rate.
        if (machine.isComplete) {
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge): confirms
            // the guard actually caught a rating attempt after completion instead of crashing.
            logCr("onRating($rating) ignored, session already complete")
            return
        }
        val cardBefore = machine.currentCard?.id
        val outcome = rate(machine, rating)
        ratedSessionState = outcome.state
        val applyEffects = {
            _state.update { it.copy(isAnswerRevealed = false) }
            syncStateFromRatedSession()
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
            logCr("onRating($rating) card=$cardBefore -> terminal=${outcome.terminal} | ${stateSnapshot()}")
            if (outcome.state.isComplete) navigateBack()
        }
        if (deferSync) pendingSessionSync = applyEffects else applyEffects()
    }

    private fun ensureVoiceGatewayStarted() {
        if (voiceStarted) return
        with(_state.value) {
            if (flashcards.isEmpty()) return
            voiceStarted = true
            // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
            logCr("ensureVoiceGatewayStarted cards=${flashcards.map { it.id }} startIndex=$currentCardIndex")
            voiceGateway.start(
                cards = flashcards,
                startIndex = currentCardIndex,
                subcategoryName = sessionTitle,
            )
        }
        voiceGateway.setSpeechRate(sessionVoiceSettings.speechRate)
        voiceGateway.setVoice(sessionVoiceSettings.voiceId)
    }

    fun onVoicePlayPause() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoicePlayPause | ${stateSnapshot()}")
        if (pausedDueToExtendedContext) {
            advanceAfterExtendedContextJob?.cancel()
            pausedDueToExtendedContext = false
            viewModelScope.launch {
                voiceGateway.rewindToNext()
                voiceGateway.togglePlayPause()
            }
        } else {
            voiceGateway.togglePlayPause()
        }
    }

    fun onVoiceNext() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoiceNext | ${stateSnapshot()}")
        advanceAfterExtendedContextJob?.cancel()
        pausedDueToExtendedContext = false
        voiceGateway.rewindToNext()
    }

    fun onVoicePrevious() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoicePrevious | ${stateSnapshot()}")
        advanceAfterExtendedContextJob?.cancel()
        pausedDueToExtendedContext = false
        if (isPastRewindThreshold || voiceGateway.state.value.currentIndex == 0) {
            voiceGateway.restartCurrentCard()
            startRewindThresholdTimer()
        } else {
            voiceGateway.rewindToPrevious()
        }
    }

    fun onVoiceSpeedChange(rate: Float) {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onVoiceSpeedChange rate=$rate")
        voiceGateway.setSpeechRate(rate)
    }

    private fun onExtendedContextDialogOpen(dialog: ExtendedContext) {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onExtendedContextDialogOpen | ${stateSnapshot()}")
        _state.update { it.copy(activeDialog = dialog) }
        val voiceState = voiceGateway.state.value
        if (voiceState.isInBetweenPause && voiceState.isPlaying) {
            pausedDueToExtendedContext = true
            viewModelScope.launch { voiceGateway.togglePlayPause() }
        }
    }

    private fun onExtendedContextDialogDismissed() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onExtendedContextDialogDismissed pausedDueToExtendedContext=$pausedDueToExtendedContext")
        if (pausedDueToExtendedContext) {
            advanceAfterExtendedContextJob = viewModelScope.launch {
                delay(EXTENDED_CONTEXT_ADVANCE_DELAY_MS)
                pausedDueToExtendedContext = false
                voiceGateway.rewindToNext()
                voiceGateway.togglePlayPause()
            }
        }
    }

    fun onVoiceErrorDismissed() {
        _state.update { it.copy(voiceError = null) }
    }

    private fun startRewindThresholdTimer() {
        rewindJob?.cancel()
        isPastRewindThreshold = false
        rewindJob = viewModelScope.launch {
            delay(rewindThresholdMs)
            isPastRewindThreshold = true
        }
    }

    private fun onVoiceSettingsOpen() {
        if (_state.value.isVoicePlaying) {
            pausedForVoiceSettings = true
            voiceGateway.togglePlayPause()
        }
        _state.update {
            it.copy(activeDialog = VoiceSettings(voiceSettingsController.seedDraft(sessionVoiceSettings)))
        }
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * The voice list arrives after the dialog is already up, so it has to find the open dialog to
     * fill in — the one narrowing cast left in the dialog path, once per open rather than once per
     * edit. A dismissal in the meantime correctly drops it.
     */
    private fun onVoicesLoaded(voices: List<VoiceOption>) {
        _state.update { state ->
            val dialog = state.activeDialog as? VoiceSettings ?: return@update state
            state.copy(
                activeDialog = dialog.copy(
                    draftState = dialog.draftState.copy(
                        availableVoices = voices,
                        draftVoiceId = dialog.draftState.draftVoiceId ?: voices.firstOrNull()?.id,
                    ),
                ),
            )
        }
    }

    /**
     * Always applies to the rest of this session; only persists as the new default when the
     * dialog's checkbox is checked (ADR-0030). Either way the preview player is done with — a save
     * stops it same as [voiceSettingsController]'s own `save` would, and an unchecked confirm has
     * no other call into the controller left to do that.
     */
    private fun onVoiceSettingsSave() {
        val dialog = _state.value.activeDialog as? VoiceSettings ?: return
        val settings = dialog.draftState.toVoiceSettings()
        sessionVoiceSettings = settings
        if (dialog.keepAsDefault) {
            voiceSettingsController.save(viewModelScope, dialog.draftState)
        } else {
            voiceSettingsController.stopPreview()
        }
        if (_state.value.isVoiceActive) {
            voiceGateway.setSpeechRate(settings.speechRate)
            voiceGateway.setVoice(settings.voiceId)
        }
        _state.update { it.copy(activeDialog = null) }
        resumeIfPausedForVoiceSettings()
    }

    private fun onVoiceSettingsDismiss() {
        voiceSettingsController.stopPreview()
        _state.update { it.copy(activeDialog = null) }
        resumeIfPausedForVoiceSettings()
    }

    private fun resumeIfPausedForVoiceSettings() {
        if (pausedForVoiceSettings) {
            pausedForVoiceSettings = false
            voiceGateway.togglePlayPause()
        }
    }

    /**
     * Single entry point for every dialog on this screen. Exit-session confirmation is the one
     * case with no ViewModel work behind it — the screen navigates and there is nothing to commit.
     */
    fun onDialogEvent(event: StudySessionDialogEvent) {
        when (event) {
            is Open -> onDialogOpen(event.dialog)
            is DraftChange -> onDraftChange(event.dialog)
            Confirm -> onDialogConfirm()
            Dismiss -> onDialogDismiss()
        }
    }

    /**
     * The caller hands over the dialog it wants shown, already seeded from what it was rendering.
     * This adds only what the call site could not: the playback side effects, and the voice-settings
     * draftState, which comes from the shared controller rather than screen state.
     */
    private fun onDialogOpen(dialog: StudySessionDialog) {
        when (dialog) {
            is ReportProblem -> onReportProblemOpen(dialog)
            is ExtendedContext -> onExtendedContextDialogOpen(dialog)
            is VoiceSettings -> onVoiceSettingsOpen()
            VoiceAnswerConsent, ExitSession ->
                _state.update { it.copy(activeDialog = dialog) }
        }
    }

    /**
     * Stores the draftState the host built, then fires any side effect the edit implies.
     *
     * The side effect comes from diffing the previous draftState against the next rather than from an
     * event that names the changed field: it keeps every dialog on the one generic
     * [StudySessionDialogEvent.DraftChange], and puts the trigger somewhere a unit test can reach
     * (ADR-0036).
     */
    private fun onDraftChange(dialog: StudySessionDialog) {
        val previous = _state.value.activeDialog
        _state.update { it.copy(activeDialog = dialog) }
        if (previous is VoiceSettings &&
            dialog is VoiceSettings &&
            dialog.draftState != previous.draftState
        ) {
            voiceSettingsController.preview(dialog.draftState)
        }
    }

    private fun onDialogConfirm() {
        when (_state.value.activeDialog) {
            is ReportProblem -> onReportProblemSubmit()
            VoiceAnswerConsent -> onVoiceAnswerConsentAccept()
            is VoiceSettings -> onVoiceSettingsSave()
            ExitSession -> {
                onDialogDismiss()
                navigateBack()
            }
            // "Got it" and a scrim tap are the same act on a single-action dialog.
            is ExtendedContext, null -> onDialogDismiss()
        }
    }

    /** Always the discard path: the draftState dies with the field. */
    private fun onDialogDismiss() {
        val dialog = _state.value.activeDialog
        _state.update { it.copy(activeDialog = null) }
        when (dialog) {
            is ExtendedContext -> onExtendedContextDialogDismissed()
            is VoiceSettings -> onVoiceSettingsDismiss()
            else -> Unit
        }
    }

    /**
     * Reporting pauses playback the way the old debug FAB did — the user stopped to read the card,
     * not to be read over. Resuming is a deliberate tap (ADR-0017).
     */
    private fun onReportProblemOpen(dialog: ReportProblem) {
        if (_state.value.isVoicePlaying) voiceGateway.togglePlayPause()
        _state.update { it.copy(activeDialog = dialog) }
    }

    private fun onReportProblemSubmit() {
        val dialog = _state.value.activeDialog as? ReportProblem ?: return
        if (!dialog.canSubmit) return
        _state.update { it.copy(activeDialog = null) }
        viewModelScope.launch {
            submitCurationReport(
                SubmitCurationReportUseCase.Params(
                    cardId = dialog.cardId,
                    subcategoryId = dialog.subcategoryId,
                    actions = dialog.selectedActions,
                )
            ).onFailure {
                _state.update { it.copy(curationError = "Failed to submit report") }
            }
        }
    }

    /** Leaving is a one-time event, never a flag in state (ADR-0019). */
    private fun navigateBack() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("navigateBack | ${stateSnapshot()}")
        viewModelScope.launch { eventChannel.send(RatedStudySessionDestination.Back) }
    }

    fun onCurationErrorDismissed() {
        _state.update { it.copy(curationError = null) }
    }

    public override fun onCleared() {
        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        logCr("onCleared | ${stateSnapshot()}")
        voiceGateway.stop()
        super.onCleared()
    }

    private companion object {
        const val EXTENDED_CONTEXT_ADVANCE_DELAY_MS = 500L
        const val CONSECUTIVE_SILENCE_PAUSE_THRESHOLD = 3

        // TEST-LOG (throwaway, PR 66 CR manual verification — remove before merge).
        const val TEST_LOG_TAG = "RatedCR"
    }
}
