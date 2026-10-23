package com.example.attendly.ai

import java.time.DayOfWeek
import java.time.LocalTime

data class ExtractedClass(
    val day: DayOfWeek,
    val subject: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val instructor: String? = null,
    val confidence: Float = 1.0f
)

data class TimetableExtractionResult(
    val classes: List<ExtractedClass>,
    val error: String? = null
)
