package com.rossomak.flashcards.core.data.mapper

import com.rossomak.flashcards.core.data.model.VoiceAnswerGradeDto
import com.rossomak.flashcards.core.domain.model.VoiceAnswerGrade

fun VoiceAnswerGradeDto.toDomain(): VoiceAnswerGrade = VoiceAnswerGrade(
    sanitizedTranscript = sanitizedTranscript,
    gradePercent = gradePercent,
    feedback = feedback
)
