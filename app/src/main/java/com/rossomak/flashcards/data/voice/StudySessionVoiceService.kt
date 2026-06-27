package com.rossomak.flashcards.data.voice

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rossomak.flashcards.MainActivity
import com.rossomak.flashcards.data.voice.StudySessionVoiceService.Companion.ACTION_BIND_LOCAL
import com.rossomak.flashcards.domain.voice.VoicePlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Media3 [MediaSessionService] that reads flashcards aloud, with background playback capabilities. It owns a
 * [TtsPlayer] (TextToSpeech wrapped as a Media3 `Player`) and a [MediaSession]; Media3 provides the
 * lock-screen / Bluetooth / notification transport controls, media-button routing and foreground
 * lifecycle. Audio focus is managed inside [TtsPlayer] because Media3 only auto-handles focus for
 * `ExoPlayer`, which we cannot use because it does not support TTS OOTB.
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
            player.loadAndStartSession(cards, startIndex, subcategoryName)

        fun togglePlayPause() = player.togglePlayPause()

        fun moveToNextCard() = player.moveToNextCard()

        fun moveToPreviousCard() = player.moveToPreviousCard()

        fun restartCurrentCardPlayback() = player.restartCurrentCardPlayback()

        fun skipToCardAnswerPlayback() = player.skipToCardAnswerPlayback()

        fun setPlaybackSpeechRate(rate: Float) = player.setPlaybackSpeechRate(rate)

        fun stopPlayback() = this@StudySessionVoiceService.stopPlayback()
    }

    override fun onCreate() {
        super.onCreate()
        player = TtsPlayer(this)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(contentPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_BIND_LOCAL) binder else super.onBind(intent)

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Voice is tied to the study screen; swiping the app away ends playback.
        stopPlayback()
        super.onTaskRemoved(rootIntent)
    }

    private fun stopPlayback() {
        player.stopPlayback()
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
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
        const val ACTION_BIND_LOCAL = "com.rossomak.flashcards.data.voice.BIND_LOCAL"
    }
}
