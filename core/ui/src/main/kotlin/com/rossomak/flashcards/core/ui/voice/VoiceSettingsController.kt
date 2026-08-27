package com.rossomak.flashcards.core.ui.voice

import android.util.Log
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoicePlayback
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.model.VoiceSettings
import com.rossomak.flashcards.core.domain.repository.VoicePreviewGateway
import com.rossomak.flashcards.core.domain.usecase.GetAvailableVoicesUseCase
import com.rossomak.flashcards.core.domain.usecase.ObserveStudySessionPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveStudySessionPreferenceUseCase
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
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

/**
 * Owns the voice list cache, preview playback and saving for the voice-settings dialog. Shared by
 * feature:study and feature:settings; each ViewModel injects its own instance (unscoped) and binds
 * it to its own viewModelScope.
 */
class VoiceSettingsController @Inject constructor(
    private val observeStudySessionPreferences: ObserveStudySessionPreferencesUseCase,
    private val saveStudySessionPreference: SaveStudySessionPreferenceUseCase,
    private val getAvailableVoices: GetAvailableVoicesUseCase,
    private val previewGateway: VoicePreviewGateway,
) {

    private var savedSettings: VoiceSettings? = null
    private var cachedVoices: List<VoiceOption>? = null
    private var loadJob: CompletableDeferred<List<VoiceOption>>? = null

    val currentSettings: VoiceSettings get() = savedSettings ?: VoiceSettings()

    /**
     * Starts the one subscription to the saved settings. [onSettingsChange] is how a screen that
     * *renders* the saved value — the Settings screen's voice row and its summary — reads it,
     * rather than collecting the same use case a second time: two subscriptions would mean two
     * copies of the same truth, one of which could be stale while the other is not.
     *
     * A screen that only opens the dialog (the study session) omits it and lets [seedDraft] read
     * the value at open time.
     */
    fun bind(scope: CoroutineScope, onSettingsChange: (VoiceSettings) -> Unit = {}) {
        scope.launch {
            observeStudySessionPreferences().map { it.voiceSettings }.collect { settings ->
                savedSettings = settings
                onSettingsChange(settings)
            }
        }
    }

    /**
     * The draft a newly opened dialog starts from — the saved settings, plus the voice list if it
     * is already cached. When [bind]'s first emission hasn't landed yet, this seeds a placeholder
     * (default speed, no voice picked) rather than a guess — same as an uncached voice list
     * starting empty. [applySavedSettings] fills the real values in once they arrive.
     */
    fun seedDraft(): VoiceSettingsDraftState {
        val voices = cachedVoices.orEmpty()
        val settings = savedSettings
        return VoiceSettingsDraftState(
            availableVoices = voices,
            draftVoiceId = settings?.voiceId ?: voices.firstOrNull()?.id,
            draftSpeed = settings?.speechRate ?: DEFAULT_SPEED,
        )
    }

    /**
     * Fills an open dialog in with the real saved values once [bind] delivers them — the
     * placeholder [seedDraft] seeds when a dialog opens before the first emission lands. Applied
     * unconditionally rather than guarded against a since-started edit: settings only change here
     * by this same dialog saving, so a second emission while it is still open does not happen in
     * practice.
     */
    fun applySavedSettings(draft: VoiceSettingsDraftState, settings: VoiceSettings): VoiceSettingsDraftState =
        draft.copy(
            draftVoiceId = settings.voiceId ?: draft.draftVoiceId,
            draftSpeed = settings.speechRate,
        )

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
        val settings = VoiceSettings(speechRate = draft.draftSpeed, voiceId = draft.draftVoiceId)
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
        const val DEFAULT_SPEED = 1f
    }
}
