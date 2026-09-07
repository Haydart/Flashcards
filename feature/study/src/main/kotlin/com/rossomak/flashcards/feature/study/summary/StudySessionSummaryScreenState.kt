package com.rossomak.flashcards.feature.study.summary

import com.rossomak.flashcards.core.domain.model.StudyMode

/**
 * Everything the Session Summary screen renders — deliberately plain (spec 03 ticket 02): mode,
 * duration, how many cards were studied, whether the deck was completed or the session was
 * abandoned, and the three Terminal State counts. [masteredCount]/[partialCount]/[failedCount] are
 * always zero for a Fast result — Fast never produces those outcomes — and the content chooses the
 * reduced Fast variant off [mode] rather than inferring it from the counts being zero.
 */
data class StudySessionSummaryScreenState(
    val mode: StudyMode = StudyMode.Rated,
    val durationSeconds: Int = 0,
    val studiedCount: Int = 0,
    val abandoned: Boolean = false,
    val masteredCount: Int = 0,
    val partialCount: Int = 0,
    val failedCount: Int = 0,
)
