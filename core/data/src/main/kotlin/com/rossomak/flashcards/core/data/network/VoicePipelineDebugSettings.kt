package com.rossomak.flashcards.core.data.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Per-stage fake/real routing state for the voice grading pipeline, driven by the debug Voice
 * screen so each fake can be replaced by the real integration and re-verified in the same
 * place it was faked. In-memory on purpose: debug-session-scoped, resets on process death.
 */
@Singleton
class VoicePipelineDebugSettings @Inject constructor() {

    data class StageToggles(
        val useRealTranscription: Boolean = false,
        val useRealGrading: Boolean = false,
        val useRealEntitlement: Boolean = false,
        /** What the *simulated* server-side entitlement record says for this user. */
        val simulatePremiumEntitlement: Boolean = true
    )

    private val _toggles = MutableStateFlow(StageToggles())
    val toggles: StateFlow<StageToggles> = _toggles.asStateFlow()

    fun setUseRealTranscription(useReal: Boolean) {
        _toggles.update { it.copy(useRealTranscription = useReal) }
    }

    fun setUseRealGrading(useReal: Boolean) {
        _toggles.update { it.copy(useRealGrading = useReal) }
    }

    fun setUseRealEntitlement(useReal: Boolean) {
        _toggles.update { it.copy(useRealEntitlement = useReal) }
    }

    fun setSimulatePremiumEntitlement(isPremium: Boolean) {
        _toggles.update { it.copy(simulatePremiumEntitlement = isPremium) }
    }
}
