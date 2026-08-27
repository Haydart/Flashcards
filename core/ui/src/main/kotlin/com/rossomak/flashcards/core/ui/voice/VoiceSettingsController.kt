package com.rossomak.flashcards.core.ui.voice

import android.util.Log
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoicePlayback
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import com.rossomak.flashcards.core.domain.usecase.GetAvailableVoicesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveStudySessionPreferenceUseCase
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The voice-settings dialog's draft.
 *
 * Lives in the screen's `activeDialog` field like every other dialog's draft (ADR-0036), which is
 * why this controller neither holds nor exposes it — a second copy here would be a second source of
 * truth, and would let the draft outlive the dialog that owns it.
 */
data class VoiceSettingsDraftState(
    val availableVoices: List<VoiceOption> = emptyList(),
    val draftVoiceId: String? = null,
    val draftSpeed: Float = 1f,
)

/** The domain value a confirmed draft folds into — what [VoiceSettingsController.save] persists. */
fun VoiceSettingsDraftState.toVoiceSettings(): VoiceSettings =
    VoiceSettings(speechRate = draftSpeed, voiceId = draftVoiceId)

/**
 * Owns the voice list cache, preview playback and saving for the voice-settings dialog. Shared by
 * feature:study and feature:settings; each ViewModel injects its own instance (unscoped) and binds
 * it to its own viewModelScope.
 *
 * Voice settings are session-scoped like every other study setting (`mode`, `ratedAttempts`): the
 * saved value lives on `StudySessionConfig`/`StudySessionRoute`, not here. A screen hands the
 * current value to [seedDraft] itself rather than this controller tracking a subscription of its
 * own — one fewer place a value could disagree with the config the screen already has in state.
 */
class VoiceSettingsController @Inject constructor(
    private val saveStudySessionPreference: SaveStudySessionPreferenceUseCase,
    private val getAvailableVoices: GetAvailableVoicesUseCase,
    private val previewGateway: VoicePreviewGateway,
) {

    private var cachedVoices: List<VoiceOption>? = null
    private var loadJob: CompletableDeferred<List<VoiceOption>>? = null

    /** The draft a newly opened dialog starts from — [current] plus the voice list if cached. */
    fun seedDraft(current: VoiceSettings): VoiceSettingsDraftState {
        val voices = cachedVoices.orEmpty()
        return VoiceSettingsDraftState(
            availableVoices = voices,
            draftVoiceId = current.voiceId ?: voices.firstOrNull()?.id,
            draftSpeed = current.speechRate,
        )
    }

    /**
     * Never re-queries the platform once the list is cached — but still calls back with the cache,
     * so a caller that needs the voices (the Settings row resolves the saved id to a display name)
     * gets them on every call rather than only on the first. Concurrent callers that land before
     * the first query resolves share its result rather than each starting their own.
     */
    fun loadVoices(scope: CoroutineScope, onLoaded: (List<VoiceOption>) -> Unit) {
        cachedVoices?.let { voices ->
            onLoaded(voices)
            return
        }
        loadJob?.let { inFlight ->
            scope.launch { onLoaded(inFlight.await()) }
            return
        }
        val deferred = CompletableDeferred<List<VoiceOption>>()
        loadJob = deferred
        scope.launch {
            val voices = runCatching { getAvailableVoices() }.getOrDefault(emptyList())
            cachedVoices = voices
            loadJob = null
            deferred.complete(voices)
            onLoaded(voices)
        }
    }

    fun preview(draft: VoiceSettingsDraftState) {
        previewGateway.preview(draft.draftVoiceId, draft.draftSpeed)
    }

    fun save(scope: CoroutineScope, draft: VoiceSettingsDraftState): VoiceSettings {
        val settings = draft.toVoiceSettings()
        scope.launch {
            saveStudySessionPreference(VoicePlayback(settings))
                .onFailure { Log.e(TAG, "Failed to save voice settings", it) }
        }
        previewGateway.stop()
        return settings
    }

    fun stopPreview() {
        previewGateway.stop()
    }

    private companion object {
        const val TAG = "VoiceSettingsController"
    }
}
