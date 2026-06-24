package com.rossomak.flashcards.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rossomak.flashcards.MainActivity
import kotlinx.coroutines.flow.StateFlow
import com.rossomak.flashcards.domain.voice.VoicePlaybackState

/**
 * Media3 [MediaSessionService] that reads flashcards aloud for the Fast voice study mode. It owns a
 * [TtsPlayer] (TextToSpeech wrapped as a Media3 `Player`) and a [MediaSession]; Media3 provides the
 * lock-screen / Bluetooth / notification transport controls, media-button routing and foreground
 * lifecycle. Audio focus is managed inside [TtsPlayer] because Media3 only auto-handles focus for
 * `ExoPlayer`.
 *
 * The in-app UI binds via [LocalBinder] (custom [ACTION_BIND_LOCAL] intent) to push the card queue
 * and drive playback, and observes [LocalBinder.state] — which carries TTS-specific phase and
 * between-card-pause flags that the standard `Player` state cannot express. System controllers
 * connect to the [MediaSession] returned from [onGetSession].
 */
@UnstableApi
class StudySessionVoiceService : MediaSessionService() {

    private val binder = LocalBinder()

    private lateinit var player: TtsPlayer
    private lateinit var mediaSession: MediaSession

    inner class LocalBinder : Binder() {
        val state: StateFlow<VoicePlaybackState> get() = player.voiceState

        fun loadSession(cards: List<VoiceCard>, startIndex: Int, subcategoryName: String) =
            player.commandLoadSession(cards, startIndex, subcategoryName)

        fun togglePlayPause() = player.commandTogglePlayPause()
        fun rewindToNext() = player.rewindToNext()
        fun rewindToPrevious() = player.rewindToPrevious()
        fun restartCurrentCard() = player.commandRestartCurrentCard()
        fun showAnswer() = player.commandShowAnswer()
        fun setSpeechRate(rate: Float) = player.commandSetSpeechRate(rate)
        fun speakExtendedContext(text: String) = player.commandSpeakExtendedContext(text)
        fun stopPlayback() = this@StudySessionVoiceService.stopPlayback()
    }

    override fun onCreate() {
        super.onCreate()
        player = TtsPlayer(this)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(contentPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_BIND_LOCAL) binder else super.onBind(intent)

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Voice is tied to the study screen; swiping the app away ends playback.
        stopPlayback()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    private fun stopPlayback() {
        player.commandStop()
        stopSelf()
    }

    private fun contentPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_BIND_LOCAL = "com.rossomak.flashcards.service.BIND_LOCAL"
    }
}
