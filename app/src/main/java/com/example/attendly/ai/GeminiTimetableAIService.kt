package com.example.attendly.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

class GeminiTimetableAIService : TimetableAIService {

    private val gson = Gson()
    private val TAG = "GeminiAIService"
    private val MODEL_NAME = "gemini-3.5-flash-lite"
    private val MAX_RETRIES = 3

    override suspend fun analyzeTimetable(imageBytes: ByteArray, apiKey: String): TimetableExtractionResult = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) return@withContext TimetableExtractionResult(emptyList(), "API Key is empty.")

        val rawBitmap = try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: throw Exception("Invalid image")
        } catch (e: Exception) {
            return@withContext TimetableExtractionResult(emptyList(), "Could not decode image.")
        }
        
        val scaledBitmap = scaleBitmap(rawBitmap)
        val base64Image = bitmapToBase64(scaledBitmap)
        val requestBody = constructPayload(base64Image)

        var currentAttempt = 0
        var lastError = ""

        while (currentAttempt <= MAX_RETRIES) {
            val result = executeRequest(trimmedKey, requestBody)
            if (result.error == null) return@withContext result

            lastError = result.error
            // Only retry on true transient server issues
            if (lastError.contains("503") || lastError.contains("504") || lastError.contains("429")) {
                currentAttempt++
                // Exponential backoff: 2s, 4s, 8s...
                val delayTime = (Math.pow(2.0, currentAttempt.toDouble()) * 1000).toLong() + (500..1500).random()
                Log.d(TAG, "Attempt $currentAttempt failed with transient error: $lastError. Retrying in ${delayTime}ms...")
                delay(delayTime)
            } else {
                break // Stop immediately for 401/403/404
            }
        }
        TimetableExtractionResult(emptyList(), lastError)
    }

    private fun executeRequest(apiKey: String, requestBody: Map<String, Any>): TimetableExtractionResult {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey)
            connection.setRequestProperty("X-Android-Package", "com.example.attendly")

            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.doOutput = true
            
            OutputStreamWriter(connection.outputStream).use { gson.toJson(requestBody, it) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseObj = gson.fromJson(responseText, JsonObject::class.java)
                val extractedText = responseObj.getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")?.get(0)?.asJsonObject?.get("text")?.asString 
                    ?: throw Exception("Missing response text")
                
                return parseResultContent(extractedText)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No info"
                Log.e(TAG, "Server Error $responseCode: $errorText")
                
                val friendlyMessage = when(responseCode) {
                    401 -> "Authentication Failed (401): Token expired. Refresh key in AI Studio."
                    403 -> "Permission Denied (403): Enable Generative Language API in Cloud Console."
                    404 -> "Model Not Found (404): The requested model is unavailable."
                    503 -> "Model Busy (503): Service experiencing high demand. Retrying..."
                    else -> "API Error ($responseCode)"
                }
                return TimetableExtractionResult(emptyList(), friendlyMessage)
            }
        } catch (e: Exception) {
            return TimetableExtractionResult(emptyList(), "Network failure: ${e.localizedMessage}")
        }
    }

    private fun constructPayload(base64Image: String): Map<String, Any> {
        val prompt = """
            Analyze this college timetable image and extract the weekly schedule.
            Return ONLY a JSON object with a "classes" array.
            
            Each class object must have:
            - "day": Day of the week (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
            - "subject": The subject name or code (e.g., DBMS, OS, Advanced Java)
            - "startTime": HH:mm format (24-hour, e.g., 09:00, 14:30)
            - "endTime": HH:mm format (24-hour, e.g., 10:00, 16:30)
            - "room": Room number or location name (null if not found)
            - "instructor": Name of the teacher/faculty (null if not found)
            - "confidence": Float between 0.0 and 1.0 based on how clear the text was
            
            Rules:
            1. ONLY extract actual academic classes.
            2. IGNORE: Lunch breaks, tea breaks, free periods, holidays, and generic headers.
            3. Do NOT invent missing info. Use null.
            4. Keep subject abbreviations exactly as they appear in the image.
            5. Handle labs: If a lab class occupies multiple time slots (e.g. 09:00-10:00 and 10:00-11:00), merge them into one entry (09:00-11:00).
            6. Ensure 'day' is a valid DayOfWeek enum string.
            7. The timetable might be a grid or a vertical list. Identify the structure before extracting.
        """.trimIndent()

        return mapOf("contents" to listOf(mapOf("parts" to listOf(
            mapOf("text" to prompt),
            mapOf("inlineData" to mapOf("mimeType" to "image/jpeg", "data" to base64Image))
        ))))
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 1200 // More aggressive scaling to ensure success with AQ tokens
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseResultContent(text: String): TimetableExtractionResult {
        val jsonString = text.substringAfter("```json").substringBeforeLast("```").trim()
            .let { if (it.contains("{")) it else text.trim() }
        try {
            val type = object : TypeToken<Map<String, List<ExtractedRawClass>>>() {}.type
            val rawResult: Map<String, List<ExtractedRawClass>> = gson.fromJson(jsonString, type)
            val classes = rawResult["classes"]?.mapNotNull { it.toExtractedClass() } ?: emptyList()
            return TimetableExtractionResult(classes = classes)
        } catch (e: Exception) {
            return TimetableExtractionResult(emptyList(), "Success! But parsing failed. Please review manually.")
        }
    }

    private data class ExtractedRawClass(
        val day: String, val subject: String, val startTime: String, 
        val endTime: String, val room: String?, val instructor: String?, val confidence: Float
    ) {
        fun toExtractedClass(): ExtractedClass? {
            return try {
                val parsedDay = when {
                    day.startsWith("MON", true) -> DayOfWeek.MONDAY
                    day.startsWith("TUE", true) -> DayOfWeek.TUESDAY
                    day.startsWith("WED", true) -> DayOfWeek.WEDNESDAY
                    day.startsWith("THU", true) -> DayOfWeek.THURSDAY
                    day.startsWith("FRI", true) -> DayOfWeek.FRIDAY
                    day.startsWith("SAT", true) -> DayOfWeek.SATURDAY
                    day.startsWith("SUN", true) -> DayOfWeek.SUNDAY
                    else -> DayOfWeek.valueOf(day.uppercase(Locale.US))
                }
                fun parseTime(s: String): LocalTime {
                    val clean = s.trim().uppercase()
                    try {
                        val parts = clean.split(":")
                        var h = parts[0].toInt()
                        val m = parts[1].take(2).toInt()
                        if (clean.contains("PM") && h < 12) h += 12
                        if (clean.contains("AM") && h == 12) h = 0
                        return LocalTime.of(h, m)
                    } catch (e: Exception) { return LocalTime.of(9, 0) }
                }
                ExtractedClass(parsedDay, subject, parseTime(startTime), parseTime(endTime), room, instructor, confidence)
            } catch (e: Exception) { null }
        }
    }
}
