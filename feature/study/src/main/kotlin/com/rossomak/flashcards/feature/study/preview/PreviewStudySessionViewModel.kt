package com.rossomak.flashcards.feature.study.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rossomak.flashcards.core.domain.model.StudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionConfig
import com.rossomak.flashcards.core.domain.model.StudySessionPreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.DefaultStudyMode
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.RatedAttempts
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.ReadAloudEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SessionLength
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SortOrder
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.SubcategoryCountRange as SubcategoryCountRangePreference
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoiceAnsweringEnabled
import com.rossomak.flashcards.core.domain.model.StudySessionPreference.VoicePlayback
import com.rossomak.flashcards.core.domain.model.VoiceOption
import com.rossomak.flashcards.core.domain.usecase.ObserveStudySessionPreferencesUseCase
import com.rossomak.flashcards.core.domain.usecase.SampleQuickSessionSubcategoriesUseCase
import com.rossomak.flashcards.core.domain.usecase.SaveStudySessionPreferenceUseCase
import com.rossomak.flashcards.core.domain.usecase.SelectSessionFlashcardsUseCase
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Confirm
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Dismiss
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.DraftChange
import com.rossomak.flashcards.core.ui.dialog.DialogEvent.Open
import com.rossomak.flashcards.core.ui.navigation.decodeRoute
import com.rossomak.flashcards.core.ui.voice.VoiceSettingsController
import com.rossomak.flashcards.core.ui.voice.toVoiceSettings
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Attempts
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Filters
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Length
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Mode
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.ReadAloud
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.Sort
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.SubcategoryCountRange
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceAnswering
import com.rossomak.flashcards.feature.study.preview.PreviewDialog.VoiceSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PreviewStudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectSessionFlashcards: SelectSessionFlashcardsUseCase,
    private val sampleQuickSessionSubcategories: SampleQuickSessionSubcategoriesUseCase,
    private val observeStudySessionPreferences: ObserveStudySessionPreferencesUseCase,
    private val saveStudySessionPreference: SaveStudySessionPreferenceUseCase,
    private val voiceSettingsController: VoiceSettingsController,
) : ViewModel() {

    private val route = savedStateHandle.decodeRoute<PreviewStudySessionRoute>()

    /** `route`'s parallel id/name lists, indexed once rather than re-scanned per sampled id. */
    private val candidateSubcategoryNamesById: Map<String, String> =
        route.subcategoryIds.zip(route.subcategoryNames).toMap()

    private val _state = MutableStateFlow(
        PreviewStudySessionScreenState(
            categoryName = route.categoryName,
            subcategoryNames = route.subcategoryNames,
            isQuickSession = route.isQuickSession,
            config = StudySessionConfig(
                subcategoryIds = route.subcategoryIds,
                tagIds = route.filterTagIds.toSet(),
                difficultyRange = route.difficultyRange,
                seed = Random.nextLong(),
            ),
        )
    )
    val state: StateFlow<PreviewStudySessionScreenState> = _state.asStateFlow()

    private val eventChannel = Channel<PreviewStudySessionDestination>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var sessionStartInFlight = false

    /**
     * The in-flight [selectCards] job, if any. A new call cancels whatever's still running so a
     * slower, stale resolution can never land after a newer one — `subcategoryIds` and
     * [selectedCardIds] would otherwise pair up across two different resolutions.
     */
    private var selectionJob: Job? = null

    internal var selectedCardIds: List<String> = emptyList()
        private set

    /**
     * Seeds the config from the user's saved defaults **before** the first [selectCards] — a
     * snapshot via [first], not a live collect: [sessionLength][StudySessionConfig.length] and
     * [sortOrder][StudySessionConfig.sortOrder] change the selection, so seeding after would
     * select twice and flash the card count, and a live collect would let a "keep as my default"
     * write from this same screen clobber the session edits the user just made. Route- and
     * session-scoped fields (subcategoryIds, tagIds, difficultyRange, seed) are left untouched —
     * filters are exempt from defaults entirely (ADR-0030).
     *
     * Sort is the one seeded field the route can override: arriving from a browsed list, the order
     * the user was just looking at wins over the saved default (ADR-0038).
     */
    init {
        viewModelScope.launch {
            val defaults = observeStudySessionPreferences().first()
            _state.update { state ->
                state.copy(
                    config = state.config.copy(
                        mode = defaults.defaultStudyMode,
                        voiceAnsweringEnabled = defaults.voiceAnsweringEnabled,
                        ratedAttempts = defaults.ratedAttempts,
                        readAloudEnabled = defaults.readAloudEnabled,
                        length = defaults.sessionLength,
                        // The route wins when it carries an order: the user already saw a list in
                        // it. Null means nothing upstream chose one, so the saved default applies.
                        sortOrder = route.sortOrder ?: defaults.sortOrder,
                        voiceSettings = defaults.voiceSettings,
                        subcategoryCountRange = defaults.subcategoryCountRange,
                    ),
                )
            }
            selectCards()
        }
        // The voice row shows the voice's name, not its id, so the list is needed before the
        // dialog is ever opened — same reason Settings loads it eagerly.
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    fun onRetry() {
        selectCards()
    }

    /** A different draw from the same pool — selection is a pure function of the config's seed. */
    fun onReshuffleSubcategories() {
        _state.update { it.copy(config = it.config.copy(seed = Random.nextLong())) }
        selectCards()
    }

    /**
     * Single entry point for every dialog on this screen.
     *
     * Voice settings is the one dialog with a side effect on open and on every edit — its draft
     * comes from [VoiceSettingsController], not from screen state, and each edit previews — so it
     * gets its own [onDialogOpen]/[onDraftChange] rather than the flat assignment every other
     * dialog on this screen uses.
     */
    fun onDialogEvent(event: PreviewDialogEvent) {
        when (event) {
            is Open -> onDialogOpen(event.dialog)
            is DraftChange -> onDraftChange(event.dialog)
            Confirm -> onDialogConfirm()
            Dismiss -> onDialogDismiss()
        }
    }

    private fun onDialogOpen(dialog: PreviewDialog) {
        when (dialog) {
            is VoiceSettings -> onVoiceSettingsOpen()
            else -> _state.update { it.copy(activeDialog = dialog) }
        }
    }

    private fun onVoiceSettingsOpen() {
        val draft = voiceSettingsController.seedDraft(_state.value.config.voiceSettings)
        _state.update { it.copy(activeDialog = VoiceSettings(draft)) }
        voiceSettingsController.loadVoices(viewModelScope, ::onVoicesLoaded)
    }

    /**
     * The voice list feeds two things: the row's summary, which needs it to turn the saved id into
     * a name, and an open voice dialog, which has to be found to be filled in — the one narrowing
     * cast left in the dialog path, once per load rather than once per edit. A dismissal in the
     * meantime correctly drops the dialog half.
     */
    private fun onVoicesLoaded(voices: List<VoiceOption>) {
        _state.update { state ->
            val withVoices = state.copy(availableVoices = voices)
            val dialog = withVoices.activeDialog as? VoiceSettings ?: return@update withVoices
            withVoices.copy(
                activeDialog = dialog.copy(
                    draft = dialog.draft.copy(
                        availableVoices = voices,
                        draftVoiceId = dialog.draft.draftVoiceId ?: voices.firstOrNull()?.id,
                    ),
                ),
            )
        }
    }

    /**
     * Stores the draft the host built, then previews the edit when it is the voice dialog — every
     * other dialog on this screen is silent.
     */
    private fun onDraftChange(dialog: PreviewDialog) {
        val previous = _state.value.activeDialog
        _state.update { it.copy(activeDialog = dialog) }
        if (previous is VoiceSettings && dialog is VoiceSettings && dialog.draft != previous.draft) {
            voiceSettingsController.preview(dialog.draft)
        }
    }

    /**
     * Dismissal is the discard path: the draft dies with the field, so nothing is applied. Preview
     * playback is stopped only when it could have been started — every other dialog is silent, and
     * stopping the shared player from one of those could cut off audio this screen never began.
     */
    private fun onDialogDismiss() {
        if (_state.value.activeDialog is VoiceSettings) {
            voiceSettingsController.stopPreview()
        }
        _state.update { it.copy(activeDialog = null) }
    }

    /**
     * The only dialog state commit path. Every dialog does the same three things: fold the draft
     * into the session config, persist it as a global default when the user checked "keep as my
     * default", and close.
     *
     * Selection re-runs on every confirm regardless of the checkbox — the header reads
     * "18 cards · ~12 min", and length, filters and sort all move it.
     */
    private fun onDialogConfirm() {
        val dialog = _state.value.activeDialog ?: return
        val updatedConfig = with(_state.value.config) {
            when (dialog) {
                is Mode -> withMode(dialog.draft)
                is VoiceAnswering -> copy(voiceAnsweringEnabled = dialog.draft)
                is Attempts -> copy(ratedAttempts = dialog.draft)
                is ReadAloud -> copy(readAloudEnabled = dialog.draft)
                is Length -> copy(length = dialog.draft)
                is Sort -> copy(sortOrder = dialog.draft)
                is SubcategoryCountRange -> copy(subcategoryCountRange = dialog.draft)
                is VoiceSettings -> copy(voiceSettings = dialog.draft.toVoiceSettings())
                is Filters -> copy(
                    tagIds = dialog.draft.selectedTags,
                    difficultyRange = dialog.draft.difficultyRange,
                )
            }
        }
        dialog.toStudySessionPreferenceIfKept()?.let { preference ->
            viewModelScope.launch { saveStudySessionPreference(preference) }
        }
        if (dialog is VoiceSettings) {
            voiceSettingsController.stopPreview()
        }
        _state.update { it.copy(config = updatedConfig, activeDialog = null) }
        selectCards()
    }

    /**
     * `voiceAnsweringEnabled` is Rated-only (ADR-0025) — reset it switching away from Rated, not
     * just gate its *display* at the read sites, since the stale value would otherwise also leak
     * into [onStartSession]'s `StudySessionRoute` payload unchanged. Its own function purely to keep
     * [onDialogConfirm]'s cyclomatic complexity under detekt's threshold.
     */
    private fun StudySessionConfig.withMode(mode: StudyMode): StudySessionConfig = copy(
        mode = mode,
        voiceAnsweringEnabled = voiceAnsweringEnabled && mode == StudyMode.Rated,
    )

    /**
     * `null` when the dialog didn't check "keep as my default" — or, for [Filters], can never
     * check it at all: tags belong to one subcategory and cannot carry to another (ADR-0030).
     */
    private fun PreviewDialog.toStudySessionPreferenceIfKept(): StudySessionPreference? = when (this) {
        is Mode -> DefaultStudyMode(draft).takeIf { keepAsDefault }
        is VoiceAnswering -> VoiceAnsweringEnabled(draft).takeIf { keepAsDefault }
        is Attempts -> RatedAttempts(draft).takeIf { keepAsDefault }
        is ReadAloud -> ReadAloudEnabled(draft).takeIf { keepAsDefault }
        is Length -> SessionLength(draft).takeIf { keepAsDefault }
        is Sort -> SortOrder(draft).takeIf { keepAsDefault }
        is SubcategoryCountRange -> SubcategoryCountRangePreference(draft).takeIf { keepAsDefault }
        is VoiceSettings -> VoicePlayback(draft.toVoiceSettings()).takeIf { keepAsDefault }
        is Filters -> null
    }

    /**
     * Restores the filters the screen was originally handed — `route.filterTagIds`/
     * `route.difficultyRange` — discarding any in-screen narrowing. Does not open the Filters
     * dialog: this is a direct reset, not a shortcut into editing.
     */
    fun onResetFilters() {
        _state.update {
            it.copy(
                config = it.config.copy(
                    tagIds = route.filterTagIds.toSet(),
                    difficultyRange = route.difficultyRange,
                ),
            )
        }
        selectCards()
    }

    fun onStartSession() {
        if (selectedCardIds.isEmpty() || sessionStartInFlight) return
        sessionStartInFlight = true
        viewModelScope.launch {
            eventChannel.send(
                PreviewStudySessionDestination.StudySession(
                    StudySessionRoute(
                        categoryId = route.categoryId,
                        sessionTitle = sessionTitle(),
                        subcategoryIds = _state.value.config.subcategoryIds,
                        cardIds = selectedCardIds,
                        studyMode = _state.value.config.mode,
                        voiceAnsweringEnabled = _state.value.config.voiceAnsweringEnabled,
                        ratedAttempts = _state.value.config.ratedAttempts,
                        readAloudEnabled = _state.value.config.readAloudEnabled,
                        speechRate = _state.value.config.voiceSettings.speechRate,
                        voiceId = _state.value.config.voiceSettings.voiceId,
                    )
                )
            )
        }
    }

    /**
     * `isLoading` is set unconditionally, not just on the initial load: [canStart] gates the Start
     * button on it, and a resample or filter change needs that same gate — otherwise Start stays
     * clickable against a [selectedCardIds] that hasn't caught up with the Subcategory set just
     * written to `config.subcategoryIds` below.
     */
    private fun selectCards() {
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val resolved = resolveSubcategories()
            _state.update { state ->
                state.copy(
                    subcategoryNames = resolved.names,
                    config = state.config.copy(
                        subcategoryIds = resolved.ids,
                        // A changed Subcategory set invalidates any tags carried from the
                        // previous one — tags belong to a single Subcategory (ADR-0030).
                        tagIds = if (resolved.ids != state.config.subcategoryIds) {
                            emptySet()
                        } else {
                            state.config.tagIds
                        },
                    ),
                )
            }
            val selectionConfig = _state.value.config.forSelection(isSingleSubcategory = _state.value.isSingleSubcategory)
            selectSessionFlashcards(selectionConfig)
                .onSuccess { plan ->
                    selectedCardIds = plan.cards.map { it.id }
                    _state.update { state ->
                        // Tags belong to one subcategory, so a multi-subcategory session has no
                        // coherent tag vocabulary to offer (ADR-0030).
                        val availableTags = if (state.isSingleSubcategory) plan.poolTags else emptyList()
                        state.copy(
                            isLoading = false,
                            error = null,
                            selectedCardCount = plan.cards.size,
                            estimatedMinutes = plan.estimatedMinutes,
                            availableTags = availableTags,
                            // Materializes "no tag filter" into every tag actually selected, the
                            // same seed SubcategoryDetails applies on its own first load — mirrored
                            // here rather than left as a dialog-open-only translation, since
                            // config.tagIds is the same field a session is drawn from (ADR-0038).
                            // Idempotent once seeded: a route-carried or user-chosen tagIds is
                            // already non-empty and is left alone.
                            config = if (state.config.tagIds.isEmpty() && availableTags.isNotEmpty()) {
                                state.config.copy(tagIds = availableTags.toSet())
                            } else {
                                state.config
                            },
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Could not load flashcards") }
                }
        }
    }

    /** A resolved Subcategory selection: ids plus the names they display under, kept together. */
    private data class ResolvedSubcategories(val ids: List<String>, val names: List<String>)

    /**
     * Every session type but Quick hands [SelectSessionFlashcardsUseCase] the fixed Subcategory
     * list the route carries. Quick is the only scenario where the Subcategory *set itself* can
     * change between resolutions: it resamples a bounded subset via
     * [SampleQuickSessionSubcategoriesUseCase], seeded off the same
     * [StudySessionConfig.seed][com.rossomak.flashcards.core.domain.model.StudySessionConfig.seed]
     * the card draw uses, so reshuffling re-rolls the sample itself, not just the draw within it
     * (ADR-0040). Sampled ids are mapped back to names through [candidateSubcategoryNamesById] —
     * the pool this resolution is allowed to draw its sample from.
     */
    private suspend fun resolveSubcategories(): ResolvedSubcategories {
        if (!route.isQuickSession) {
            return ResolvedSubcategories(route.subcategoryIds, route.subcategoryNames)
        }
        val sampledIds = sampleQuickSessionSubcategories(
            SampleQuickSessionSubcategoriesUseCase.Params(
                candidateSubcategoryIds = route.subcategoryIds,
                countRange = _state.value.config.subcategoryCountRange,
                seed = _state.value.config.seed,
            )
        )
        val sampledNames = sampledIds.map { id -> candidateSubcategoryNamesById.getValue(id) }
        return ResolvedSubcategories(sampledIds, sampledNames)
    }

    private fun sessionTitle(): String =
        if (_state.value.isSingleSubcategory) _state.value.subcategoryNames.first() else route.categoryName

    /**
     * Tags belong to one subcategory, so a multi-Subcategory pool has no coherent tag vocabulary
     * to filter by at all — asserted here rather than relied on staying empty by omission
     * elsewhere (ADR-0030).
     */
    private fun StudySessionConfig.forSelection(isSingleSubcategory: Boolean): StudySessionConfig =
        if (isSingleSubcategory) this else copy(tagIds = emptySet())
}
