package com.rossomak.flashcards.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.rossomak.flashcards.MainActivity
import com.rossomak.flashcards.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Foreground service that reads flashcards aloud with the system [TextToSpeech] for the Fast
 * voice study mode. Owns the TTS engine, a [MediaSessionCompat] (lock-screen / bluetooth transport)
 * and a [NotificationCompat.MediaStyle] notification so playback survives the screen being off or
 * the app being backgrounded.
 *
 * Per card the sequence is: speak question -> pause -> speak answer -> pause -> next card. The
 * ViewModel binds via [LocalBinder], pushes the card queue with [loadSession], observes [state],
 * and drives playback through the command methods.
 */
class StudySessionVoiceService : Service() {

    private val binder = LocalBinder()

    private val _state = MutableStateFlow(VoicePlaybackState())

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var startWhenReady = false

    private lateinit var mediaSession: MediaSessionCompat

    private val handler = Handler(Looper.getMainLooper())

    private var cards: List<VoiceCard> = emptyList()
    private var index = 0
    private var phase = VoicePhase.QUESTION
    private var isPlaying = false
    private var speechRate = VoicePlaybackState.DEFAULT_SPEECH_RATE
    private var subcategoryName = ""

    /**
     * Incremented on every new utterance and on every interrupting command. An [onDone] callback
     * whose embedded generation no longer matches has been superseded (e.g. by a pause or skip) and
     * is ignored — this is how we tell a naturally finished utterance apart from a stopped one.
     */
    private var generation = 0

    inner class LocalBinder : Binder() {
        // getter, not an initializer: the binder field is constructed before _state, so capturing
        // _state eagerly here would bind to null. Defer access until first read.
        val state: StateFlow<VoicePlaybackState> get() = _state.asStateFlow()

        fun loadSession(cards: List<VoiceCard>, startIndex: Int, subcategoryName: String) =
            this@StudySessionVoiceService.loadSession(cards, startIndex, subcategoryName)

        fun togglePlayPause() = this@StudySessionVoiceService.togglePlayPause()
        fun skipNext() = this@StudySessionVoiceService.skipNext()
        fun skipPrevious() = this@StudySessionVoiceService.skipPrevious()
        fun showAnswer() = this@StudySessionVoiceService.showAnswer()
        fun setSpeechRate(rate: Float) = this@StudySessionVoiceService.setSpeechRate(rate)
        fun stopPlayback() = this@StudySessionVoiceService.stopPlayback()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setUpMediaSession()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                runCatching { tts.language = Locale.US } // app supports English voice only
                tts.setSpeechRate(speechRate)
                tts.setOnUtteranceProgressListener(utteranceListener)
                if (startWhenReady) {
                    startWhenReady = false
                    speakQuestion()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean = true

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        super.onDestroy()
    }

    // region commands

    private fun loadSession(cards: List<VoiceCard>, startIndex: Int, subcategoryName: String) {
        this.cards = cards
        this.subcategoryName = subcategoryName
        this.index = if (cards.isEmpty()) 0 else startIndex.coerceIn(0, cards.lastIndex)
        this.phase = VoicePhase.QUESTION
        startForegroundNotification()
        if (cards.isEmpty()) {
            pushState()
            return
        }
        if (ttsReady) speakQuestion() else startWhenReady = true
    }

    private fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    private fun play() {
        if (cards.isEmpty()) return
        when (phase) {
            VoicePhase.QUESTION -> speakQuestion()
            VoicePhase.ANSWER -> speakAnswer()
        }
    }

    private fun pause() {
        isPlaying = false
        generation++ // invalidate the in-flight utterance callback
        if (::tts.isInitialized) tts.stop()
        pushState()
    }

    private fun skipNext() {
        if (index < cards.lastIndex) index++
        moveToQuestion()
    }

    private fun skipPrevious() {
        if (index > 0) index--
        moveToQuestion()
    }

    /** Move to the question of the current [index]; keep playing if we were playing, else just show it. */
    private fun moveToQuestion() {
        phase = VoicePhase.QUESTION
        if (isPlaying) {
            speakQuestion()
        } else {
            generation++
            if (::tts.isInitialized) tts.stop()
            pushState()
        }
    }

    private fun showAnswer() {
        if (cards.isEmpty()) return
        speakAnswer() // interrupts the question and reads the answer immediately
    }

    private fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(VoicePlaybackState.MIN_SPEECH_RATE, VoicePlaybackState.MAX_SPEECH_RATE)
        if (::tts.isInitialized) tts.setSpeechRate(speechRate)
        if (isPlaying) {
            when (phase) {
                VoicePhase.QUESTION -> speakQuestion()
                VoicePhase.ANSWER -> speakAnswer()
            }
        } else {
            pushState()
        }
    }

    private fun stopPlayback() {
        isPlaying = false
        generation++
        if (::tts.isInitialized) tts.stop()
        cards = emptyList()
        _state.value = VoicePlaybackState(isActive = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // endregion

    // region playback sequence

    private fun speakQuestion() {
        val card = cards.getOrNull(index) ?: return
        phase = VoicePhase.QUESTION
        isPlaying = true
        val generationId = ++generation
        pushState()
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
        pushState()
        tts.speak(
            card.spokenAnswer.ifBlank { " " },
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId(TAG_ANSWER, generationId),
        )
    }

    private fun silence(durationMs: Long, tag: String) {
        val generationId = ++generation
        tts.playSilentUtterance(durationMs, TextToSpeech.QUEUE_FLUSH, utteranceId(tag, generationId))
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
            TAG_ANSWER -> silence(ANSWER_TO_NEXT_PAUSE_MS, TAG_BETWEEN)
            TAG_BETWEEN -> advanceAfterCard()
        }
    }

    private fun advanceAfterCard() {
        if (index < cards.lastIndex) {
            index++
            speakQuestion()
        } else {
            isPlaying = false // reached the end; stay foreground in a paused state
            pushState()
        }
    }

    // endregion

    // region media session + notification

    private fun setUpMediaSession() {
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG).apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = play()
                override fun onPause() = pause()
                override fun onSkipToNext() = skipNext()
                override fun onSkipToPrevious() = skipPrevious()
                override fun onStop() = stopPlayback()
            })
            isActive = true
        }
    }

    private fun pushState() {
        _state.value = VoicePlaybackState(
            isActive = cards.isNotEmpty(),
            isPlaying = isPlaying,
            currentIndex = index,
            totalCards = cards.size,
            phase = phase,
            speechRate = speechRate,
        )
        updateMediaSession()
        if (cards.isNotEmpty()) startForegroundNotification()
    }

    private fun updateMediaSession() {
        val playbackState = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(playbackState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, speechRate)
                .build()
        )
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, subcategoryName)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, subcategoryName)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, cardPositionText())
                .build()
        )
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val playPauseAction = if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(subcategoryName.ifBlank { "Study session" })
            .setContentText(cardPositionText())
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS),
            )
            .addAction(
                playPauseIcon,
                playPauseTitle,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, playPauseAction),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT),
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun cardPositionText(): String =
        if (cards.isEmpty()) "" else "${index + 1} / ${cards.size}"

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Voice study playback",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Controls for spoken flashcard playback"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // endregion

    private companion object {
        const val CHANNEL_ID = "study_voice_playback"
        const val NOTIFICATION_ID = 1001
        const val MEDIA_SESSION_TAG = "StudySessionVoice"

        const val QUESTION_TO_ANSWER_PAUSE_MS = 1_500L
        const val ANSWER_TO_NEXT_PAUSE_MS = 2_500L

        const val SEPARATOR = ":"
        const val TAG_QUESTION = "question"
        const val TAG_PAUSE = "pause"
        const val TAG_ANSWER = "answer"
        const val TAG_BETWEEN = "between"

        fun utteranceId(tag: String, generation: Int): String = "$tag$SEPARATOR$generation"
    }
}
