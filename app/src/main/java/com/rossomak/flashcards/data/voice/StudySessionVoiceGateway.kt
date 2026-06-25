package com.rossomak.flashcards.data.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rossomak.flashcards.domain.model.Flashcard
import com.rossomak.flashcards.domain.voice.VoiceGateway
import com.rossomak.flashcards.domain.voice.VoicePlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
class StudySessionVoiceGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceGateway {

    private val _state = MutableStateFlow(VoicePlaybackState())
    override val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var voiceBinder: StudySessionVoiceService.LocalBinder? = null
    private var voiceStateJob: Job? = null
    private var isBound = false

    // The LocalBinder carries playback state and commands, but MediaSessionService only registers
    // its session (and thus shows the notification / lock-screen controls / goes foreground) once a
    // MediaController connects via onGetSession. This controller exists purely to activate that
    // system transport surface; commands and state still flow through the binder.
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var pendingCards: List<VoiceCard> = emptyList()
    private var pendingStartIndex: Int = 0
    private var pendingSubcategoryName: String = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (!isBound) {
                // Rapid toggle: stop() fired before connection landed — kill the orphaned service.
                (service as? StudySessionVoiceService.LocalBinder)?.stopPlayback()
                return
            }
            val binder = service as? StudySessionVoiceService.LocalBinder ?: return
            voiceBinder = binder
            binder.loadSession(pendingCards, pendingStartIndex, pendingSubcategoryName)
            collectVoiceState(binder)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceBinder = null
        }
    }

    override fun start(
        cards: List<Flashcard>,
        startIndex: Int,
        subcategoryName: String,
    ) {
        pendingCards = cards.toVoiceCards()
        pendingStartIndex = startIndex
        pendingSubcategoryName = subcategoryName
        // Bind only: MediaSessionService promotes itself to a foreground service when playback
        // starts, so an explicit startForegroundService here would risk a 5s FGS-timeout ANR.
        val intent = Intent(context, StudySessionVoiceService::class.java).apply {
            action = StudySessionVoiceService.ACTION_BIND_LOCAL
        }
        isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        connectMediaController()
    }

    override fun stop() {
        voiceBinder?.stopPlayback()
        unbind()
        _state.value = VoicePlaybackState()
    }

    override fun togglePlayPause() {
        voiceBinder?.togglePlayPause()
    }

    override fun rewindToNext() {
        voiceBinder?.moveToNextCard()
    }

    override fun rewindToPrevious() {
        voiceBinder?.moveToPreviousCard()
    }

    override fun restartCurrentCard() {
        voiceBinder?.restartCurrentCardPlayback()
    }

    override fun showAnswer() {
        voiceBinder?.skipToCardAnswerPlayback()
    }

    override fun setSpeechRate(rate: Float) {
        voiceBinder?.setPlaybackSpeechRate(rate)
    }

    private fun collectVoiceState(binder: StudySessionVoiceService.LocalBinder) {
        voiceStateJob?.cancel()
        voiceStateJob = scope.launch {
            binder.state.collect { voice ->
                if (voice.error != null) {
                    unbind()
                    _state.value = VoicePlaybackState(error = voice.error)
                    return@collect
                }
                _state.value = voice
            }
        }
    }

    private fun connectMediaController() {
        if (controllerFuture != null) return
        val token =
            SessionToken(context, ComponentName(context, StudySessionVoiceService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
    }

    private fun releaseMediaController() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun unbind() {
        voiceStateJob?.cancel()
        voiceStateJob = null
        releaseMediaController()
        if (isBound) {
            runCatching { context.unbindService(serviceConnection) }
            isBound = false
        }
        voiceBinder = null
    }

    private fun List<Flashcard>.toVoiceCards(): List<VoiceCard> = map { card ->
        VoiceCard(
            spokenQuestion = (card.questionSpoken?.takeIf { it.isNotBlank() }
                ?: card.question).forSpeech(),
            spokenAnswer = (card.answerSpoken?.takeIf { it.isNotBlank() }
                ?: card.answer).forSpeech(),
        )
    }

    private fun String.forSpeech(): String {
        val codeTransformed = replace(Regex("`([^`]*)`")) { match ->
            match.groupValues[1]
                .replace(Regex("(?<=\\S)<([^>]+)>")) { " of ${it.groupValues[1]}" }
                .replace(Regex("(?<!\\.)\\.(?!\\.)"), " DOT ")
                .replace("_", " ")
                .replace(Regex(" {2,}"), " ")
                .trim()
        }
        return codeTransformed.replace("`", "'")
    }
}
