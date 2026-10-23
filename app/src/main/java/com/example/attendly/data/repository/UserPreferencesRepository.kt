package com.example.attendly.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.attendly.data.network.BackendApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class UserPreferencesRepository(
    context: Context,
    private val backendApiService: BackendApiService = BackendApiService()
) {
    private val sharedPreferences = context.getSharedPreferences("attendly_prefs", Context.MODE_PRIVATE)

    private val _displayNameFlow = MutableStateFlow(getDisplayName())
    val displayNameFlow: StateFlow<String?> = _displayNameFlow.asStateFlow()

    fun getInstallationId(): String {
        val existing = sharedPreferences.getString(KEY_INSTALLATION_ID, null)
        if (!existing.isNullOrEmpty()) return existing
        val newId = UUID.randomUUID().toString()
        sharedPreferences.edit { putString(KEY_INSTALLATION_ID, newId) }
        return newId
    }

    fun getDisplayName(): String? {
        return sharedPreferences.getString(KEY_DISPLAY_NAME, null)
    }

    fun setDisplayName(name: String) {
        val trimmed = name.trim()
        sharedPreferences.edit {
            putString(KEY_DISPLAY_NAME, trimmed)
        }
        _displayNameFlow.value = trimmed
    }

    fun isRegistrationCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_REGISTRATION_COMPLETED, false)
    }

    fun setRegistrationCompleted(completed: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_REGISTRATION_COMPLETED, completed)
        }
    }

    suspend fun registerUser(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be blank"))
        
        val installationId = getInstallationId()
        val apiResult = backendApiService.registerInstallation(installationId, trimmedName)
        
        if (apiResult.isSuccess) {
            setDisplayName(trimmedName)
            setRegistrationCompleted(true)
            Result.success(Unit)
        } else {
            apiResult
        }
    }

    suspend fun updateDisplayName(name: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return@withContext Result.failure(IllegalArgumentException("Name cannot be blank"))
        
        // Update local name immediately (offline-first)
        setDisplayName(trimmedName)

        val installationId = getInstallationId()
        val apiResult = backendApiService.updateDisplayName(installationId, trimmedName)
        if (apiResult.isSuccess) {
            Result.success(Unit)
        } else {
            apiResult
        }
    }

    fun getGeminiApiKey(): String? {
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, null)
    }

    fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit {
            putString(KEY_GEMINI_API_KEY, apiKey)
        }
    }

    fun clearGeminiApiKey() {
        sharedPreferences.edit {
            remove(KEY_GEMINI_API_KEY)
        }
    }

    // Attendance Reminders Preferences (OFF by default)
    fun isRemindersEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMINDERS_ENABLED, false)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(KEY_REMINDERS_ENABLED, enabled)
            if (!enabled) {
                remove(KEY_LAST_REMINDER_DATE)
            }
        }
    }

    fun getReminderHour(): Int {
        return sharedPreferences.getInt(KEY_REMINDER_HOUR, 20) // default 8:00 PM
    }

    fun getReminderMinute(): Int {
        return sharedPreferences.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        sharedPreferences.edit {
            putInt(KEY_REMINDER_HOUR, hour)
            putInt(KEY_REMINDER_MINUTE, minute)
            remove(KEY_LAST_REMINDER_DATE)
        }
    }

    // Legacy aliases for backwards compatibility
    fun getCustomReminderHour(): Int = getReminderHour()
    fun getCustomReminderMinute(): Int = getReminderMinute()
    fun setCustomReminderTime(hour: Int, minute: Int) = setReminderTime(hour, minute)

    fun getLastReminderSentDate(): String? {
        return sharedPreferences.getString(KEY_LAST_REMINDER_DATE, null)
    }

    fun setLastReminderSentDate(dateStr: String) {
        sharedPreferences.edit {
            putString(KEY_LAST_REMINDER_DATE, dateStr)
        }
    }

    fun clearLastReminderSentDate() {
        sharedPreferences.edit {
            remove(KEY_LAST_REMINDER_DATE)
        }
    }

    // JSON Export Timestamp Persistence
    fun getLastSuccessfulBackupDate(): String? {
        return sharedPreferences.getString(KEY_LAST_SUCCESSFUL_BACKUP_DATE, null)
    }

    fun setLastSuccessfulBackupDate(dateStr: String) {
        sharedPreferences.edit {
            putString(KEY_LAST_SUCCESSFUL_BACKUP_DATE, dateStr)
        }
    }

    companion object {
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_REGISTRATION_COMPLETED = "registration_completed"
        private const val KEY_INSTALLATION_ID = "installation_id"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_LAST_REMINDER_DATE = "last_reminder_date"
        private const val KEY_LAST_SUCCESSFUL_BACKUP_DATE = "last_successful_backup_date"
    }
}
