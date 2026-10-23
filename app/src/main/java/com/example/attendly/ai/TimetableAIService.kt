package com.example.attendly.ai

interface TimetableAIService {
    suspend fun analyzeTimetable(imageBytes: ByteArray, apiKey: String): TimetableExtractionResult
}
