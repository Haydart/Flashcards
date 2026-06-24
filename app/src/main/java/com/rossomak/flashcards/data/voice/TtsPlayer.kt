package com.rossomak.flashcards.data.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rossomak.flashcards.domain.voice.VoicePhase
import com.rossomak.flashcards.domain.voice.VoicePlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Media3 [SimpleBasePlayer] that reads flashcards aloud with the system [TextToSpeech] engine and
 * surfaces playback to a `MediaSession` (lock screen / Bluetooth / notification transport controls,
 * audio focus). It owns the TTS engine and the per-card sequence:
 * speak question -> pause -> speak answer -> pause -> next card.
 *
 * Two entry points drive the same internal state, then call [publishState] to refresh both the Media3
 * state and the [voiceState] side-channel:
 *  - System transport controls route through Media3 into the `handle*` overrides.
 *  - The in-app UI routes through [StudySessionVoiceService.LocalBinder] into the `command*` methods.
 *
 * [voiceState] carries TTS-specific fields ([VoicePhase], between-card pause) that the standard
 * [Player] state cannot express; the ViewModel observes it via the binder.
 */
@UnstableApi
class TtsPlayer(context: Context) : SimpleBasePlayer(Looper.getMainLooper()) {

    private val _voiceState = MutableStateFlow(VoicePlaybackState())
    val voiceState: StateFlow<VoicePlaybackState> = _voiceState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false

    private var ttsReady = false
    private var startWhenReady = false

    private var cards: List<VoiceCard> = emptyList()
    private var index = 0
    private var phase = VoicePhase.QUESTION
    private var isPlaying = false
    private var isBetweenPause = false
    private var speechRate = VoicePlaybackState.DEFAULT_SPEECH_RATE
    private var subcategoryName = ""

    /**
     * Incremented on every new utterance and every interrupting command. An [onDone] callback whose
     * embedded generation no longer matches has been superseded (e.g. by a pause or skip) and is
     * ignored — this is how a naturally finished utterance is told apart from a stopped one.
     */
    private var generation = 0
    private var cardStartedAtMs = 0L

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            runCatching {
                tts.language = Locale.US // app supports English voice only
            }
            tts.setSpeechRate(speechRate)
            tts.setOnUtteranceProgressListener(utteranceListener)
            if (startWhenReady) {
                startWhenReady = false
                speakQuestion()
            }
        } else {
            _voiceState.value = VoicePlaybackState(error = ERROR_TTS_UNAVAILABLE)
        }
    }

    /**
     * Media3 manages audio focus only for [androidx.media3.exoplayer.ExoPlayer]; a custom
     * [SimpleBasePlayer] must do it manually. Focus is requested once and held for the playback
     * lifetime (never abandoned on pause — abandoning lets a defensively-paused app such as Spotify
     * grab the slot), then auto-pauses on loss and auto-resumes on the following gain.
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeFocusLoss) {
                    wasPlayingBeforeFocusLoss = false
                    doPlay()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (isPlaying) {
                    wasPlayingBeforeFocusLoss = true
                    doPause() // stay registered — AUDIOFOCUS_GAIN fires when the other app releases
                }
            }
        }
    }

    // region Media3 Player state

    override fun getState(): State {
        val items = cards.mapIndexed { cardIndex, _ ->
            MediaItemData.Builder("card-$cardIndex")
                .setMediaItem(MediaItem.Builder().setMediaId("card-$cardIndex").build())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(subcategoryName.ifBlank { DEFAULT_TITLE })
                        .setArtist("${cardIndex + 1} / ${cards.size}")
                        .build()
                )
                .build()
        }
        return State.Builder()
            .setAvailableCommands(AVAILABLE_COMMANDS)
            .setPlaybackState(if (cards.isEmpty()) Player.STATE_IDLE else Player.STATE_READY)
            .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaylist(items)
            .setCurrentMediaItemIndex(index.coerceIn(0, maxOf(0, cards.lastIndex)))
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) doPlay() else doPause()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleStop(): ListenableFuture<*> {
        doStop()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        releaseEngine()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> doRewindToNext()
            Player.COMMAND_SEEK_TO_PREVIOUS -> doSmartPrevious() // system back: rewind-or-previous
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> doRewindToPrevious()
            else -> if (mediaItemIndex != index) jumpTo(mediaItemIndex)
        }
        return Futures.immediateVoidFuture()
    }

    fun commandLoadSession(cards: List<VoiceCard>, startIndex: Int, subcategoryName: String) {
        this.cards = cards
        this.subcategoryName = subcategoryName
        this.index = if (cards.isEmpty()) 0 else startIndex.coerceIn(0, cards.lastIndex)
        this.phase = VoicePhase.QUESTION
        this.isBetweenPause = false
        if (cards.isEmpty()) {
            publishState()
            return
        }
        cardStartedAtMs = SystemClock.elapsedRealtime()
        if (ttsReady) speakQuestion() else startWhenReady = true
    }

    fun togglePlayPause() = if (isPlaying) doPause() else doPlay()

    fun moveToNextCard() = doRewindToNext()

    fun moveToPreviousCard() = doRewindToPrevious()

    fun restartCurrentCardPlayback() = moveToQuestion()

    fun skipToCardAnswerPlayback() {
        if (cards.isEmpty()) return
        phase = VoicePhase.ANSWER
        if (isPlaying) {
            speakAnswer()
        } else {
            stopUtterance()
            publishState()
        }
    }

    fun setPlaybackSpeechRate(rate: Float) {
        speechRate =
            rate.coerceIn(VoicePlaybackState.MIN_SPEECH_RATE, VoicePlaybackState.MAX_SPEECH_RATE)
        if (ttsReady) tts.setSpeechRate(speechRate)
        if (isPlaying) {
            when (phase) {
                VoicePhase.QUESTION -> speakQuestion()
                VoicePhase.ANSWER -> speakAnswer()
            }
        } else {
            publishState()
        }
    }

    fun stopPlayback() = doStop()

    private fun doPlay() {
        if (cards.isEmpty()) return
        when (phase) {
            VoicePhase.QUESTION -> speakQuestion()
            VoicePhase.ANSWER -> speakAnswer()
        }
    }

    private fun doPause() {
        isPlaying = false
        stopUtterance()
        publishState()
    }

    private fun doRewindToNext() {
        if (index >= cards.lastIndex) return
        index++
        moveToQuestion()
    }

    private fun doRewindToPrevious() {
        if (index <= 0) return
        index--
        moveToQuestion()
    }

    /** Rewind-or-previous used by system transport: within the threshold, jump to the previous card. */
    private fun doSmartPrevious() {
        val elapsed = SystemClock.elapsedRealtime() - cardStartedAtMs
        if (elapsed < VoicePlaybackState.REWIND_THRESHOLD_MS && index > 0) {
            index--
        }
        moveToQuestion()
    }

    private fun jumpTo(targetIndex: Int) {
        index = targetIndex.coerceIn(0, cards.lastIndex)
        moveToQuestion()
    }

    /** Move to the question of the current [index]; keep playing if we were, else just show it. */
    private fun moveToQuestion() {
        phase = VoicePhase.QUESTION
        isBetweenPause = false
        cardStartedAtMs = SystemClock.elapsedRealtime()
        if (isPlaying) {
            speakQuestion()
        } else {
            stopUtterance()
            publishState()
        }
    }

    private fun doStop() {
        isPlaying = false
        startWhenReady = false
        generation++
        stopUtterance()
        abandonAudioFocus()
        cards = emptyList()
        index = 0
        isBetweenPause = false
        _voiceState.value = VoicePlaybackState(isActive = false)
        invalidateState()
    }

    private fun speakQuestion() {
        val card = cards.getOrNull(index) ?: return
        phase = VoicePhase.QUESTION
        isPlaying = true
        val generationId = ++generation
        requestAudioFocus()
        publishState()
        tts.speak(
            card.spokenQuestion.ifBlank { " " },
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId(TAG_QUESTION, generationId),
        )
    }

    private fun speakAnswer() {
        val card = cards.getOrNull(index) ?: return
        phase = VoicePhase.ANSWER
        isPlaying = true
        val generationId = ++generation
        requestAudioFocus()
        publishState()
        tts.speak(
            card.spokenAnswer.ifBlank { " " },
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId(TAG_ANSWER, generationId),
        )
    }

    private fun silence(durationMs: Long, tag: String) {
        val generationId = ++generation
        tts.playSilentUtterance(
            durationMs,
            TextToSpeech.QUEUE_FLUSH,
            utteranceId(tag, generationId)
        )
    }

    private fun advanceAfterCard() {
        if (index < cards.lastIndex) {
            index++
            isBetweenPause = false
            cardStartedAtMs = SystemClock.elapsedRealtime()
            speakQuestion()
        } else {
            phase =
                VoicePhase.QUESTION // reset so tapping Play re-reads last card from the question
            isPlaying = false
            publishState()
        }
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utteranceId ?: return
            handler.post { onUtteranceDone(utteranceId) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = Unit
    }

    private fun onUtteranceDone(utteranceId: String) {
        val generationId = utteranceId.substringAfterLast(SEPARATOR).toIntOrNull() ?: return
        if (generationId != generation) return // superseded by a newer command/utterance
        when (utteranceId.substringBefore(SEPARATOR)) {
            TAG_QUESTION -> silence(QUESTION_TO_ANSWER_PAUSE_MS, TAG_PAUSE)
            TAG_PAUSE -> speakAnswer()
            TAG_ANSWER -> {
                isBetweenPause = true
                publishState()
                silence(ANSWER_TO_NEXT_PAUSE_MS, TAG_BETWEEN)
            }

            TAG_BETWEEN -> {
                isBetweenPause = false
                advanceAfterCard()
            }
        }
    }

    private fun stopUtterance() {
        generation++ // invalidate the in-flight utterance callback
        if (ttsReady) tts.stop()
    }

    private fun releaseEngine() {
        generation++
        handler.removeCallbacksAndMessages(null)
        abandonAudioFocus()
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }

    /** Push the current internal state to both the Media3 [getState] and the [voiceState] flow. */
    private fun publishState() {
        _voiceState.value = VoicePlaybackState(
            isActive = cards.isNotEmpty(),
            isPlaying = isPlaying,
            currentIndex = index,
            totalCards = cards.size,
            phase = phase,
            isInBetweenPause = isBetweenPause,
            speechRate = speechRate,
        )
        invalidateState()
    }

    /** Request focus once and keep it; a no-op if already held (guards against per-utterance churn). */
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) return
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(true) // queue instead of fail when another app holds focus
                .setOnAudioFocusChangeListener(audioFocusListener, handler)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    // endregion

    private companion object {
        const val ERROR_TTS_UNAVAILABLE = "tts_unavailable"
        const val DEFAULT_TITLE = "Study session"

        const val QUESTION_TO_ANSWER_PAUSE_MS = 1_500L
        const val ANSWER_TO_NEXT_PAUSE_MS = 2_500L

        const val SEPARATOR = ":"
        const val TAG_QUESTION = "question"
        const val TAG_PAUSE = "pause"
        const val TAG_ANSWER = "answer"
        const val TAG_BETWEEN = "between"

        val AVAILABLE_COMMANDS = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_RELEASE,
            )
            .build()

        fun utteranceId(tag: String, generation: Int): String = "$tag$SEPARATOR$generation"
    }
}
