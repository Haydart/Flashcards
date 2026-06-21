package com.rossomak.flashcards.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
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

class StudySessionVoiceGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : VoiceGateway {

    private val _state = MutableStateFlow(VoicePlaybackState())
    override val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var voiceBinder: StudySessionVoiceService.LocalBinder? = null
    private var voiceStateJob: Job? = null
    private var isBound = false

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
        val intent = Intent(context, StudySessionVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun stop() {
        voiceBinder?.stopPlayback()
        unbind()
        _state.value = VoicePlaybackState()
    }

    override fun togglePlayPause() { voiceBinder?.togglePlayPause() }
    override fun skipNext() { voiceBinder?.skipNext() }
    override fun skipPrevious() { voiceBinder?.skipPrevious() }
    override fun showAnswer() { voiceBinder?.showAnswer() }
    override fun setSpeechRate(rate: Float) { voiceBinder?.setSpeechRate(rate) }

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

    private fun unbind() {
        voiceStateJob?.cancel()
        voiceStateJob = null
        if (isBound) {
            runCatching { context.unbindService(serviceConnection) }
            isBound = false
        }
        voiceBinder = null
    }

    private fun List<Flashcard>.toVoiceCards(): List<VoiceCard> = map { card ->
        VoiceCard(
            spokenQuestion = (card.questionSpoken?.takeIf { it.isNotBlank() } ?: card.question).forSpeech(),
            spokenAnswer = (card.answerSpoken?.takeIf { it.isNotBlank() } ?: card.answer).forSpeech(),
        )
    }

    private fun String.forSpeech(): String = replace("`", "")
}
