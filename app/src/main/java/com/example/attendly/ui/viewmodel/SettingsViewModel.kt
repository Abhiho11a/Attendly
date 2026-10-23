package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SettingsUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val lastBackupDateText: String = "No backup saved yet",
    val displayName: String = ""
) {
    val customHour: Int get() = reminderHour
    val customMinute: Int get() = reminderMinute
    val reminderMode: String get() = "single"
}

class SettingsViewModel(
    private val repository: AttendlyRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        val remindersOn = preferencesRepository?.isRemindersEnabled() ?: false
        val hour = preferencesRepository?.getReminderHour() ?: 20
        val minute = preferencesRepository?.getReminderMinute() ?: 0

        val lastBackupRaw = preferencesRepository?.getLastSuccessfulBackupDate()
        val formattedLastBackup = formatBackupDate(lastBackupRaw)

        val name = preferencesRepository?.getDisplayName() ?: ""

        _uiState.update { 
            it.copy(
                remindersEnabled = remindersOn,
                reminderHour = hour,
                reminderMinute = minute,
                lastBackupDateText = formattedLastBackup,
                displayName = name
            )
        }

        preferencesRepository?.displayNameFlow?.onEach { currentName ->
            _uiState.update { it.copy(displayName = currentName ?: "") }
        }?.launchIn(viewModelScope)

        repository.allSubjects.onEach { subjects ->
            _uiState.update { it.copy(subjects = subjects) }
        }.launchIn(viewModelScope)
    }

    private fun formatBackupDate(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrBlank()) return "No backup saved yet"
        return try {
            if (dateTimeStr.contains("T")) {
                val dt = LocalDateTime.parse(dateTimeStr)
                val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.getDefault())
                dt.format(formatter)
            } else {
                val date = LocalDate.parse(dateTimeStr)
                val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                date.format(formatter)
            }
        } catch (_: Exception) {
            "No backup saved yet"
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            val result = preferencesRepository?.updateDisplayName(newName)
            if (result != null && result.isSuccess) {
                _events.emit("Display name updated successfully! ✨")
            } else {
                _events.emit("Display name updated locally. Will sync online when connected 🌐")
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        preferencesRepository?.setRemindersEnabled(enabled)
        _uiState.update { it.copy(remindersEnabled = enabled) }
        viewModelScope.launch {
            _events.emit(if (enabled) "Attendance reminders enabled 🔔" else "Attendance reminders disabled 🔕")
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        preferencesRepository?.setReminderTime(hour, minute)
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        viewModelScope.launch {
            _events.emit("Reminder time set to $formattedTime ⏰")
        }
    }

    fun setCustomReminderTime(hour: Int, minute: Int) = setReminderTime(hour, minute)

    fun onBackupExportSuccessful() {
        val nowStr = LocalDateTime.now().toString()
        preferencesRepository?.setLastSuccessfulBackupDate(nowStr)
        val formatted = formatBackupDate(nowStr)
        _uiState.update { it.copy(lastBackupDateText = formatted) }
        viewModelScope.launch {
            _events.emit("JSON backup exported successfully! 📁")
        }
    }

    fun updateSubjectTarget(subject: SubjectEntity, target: Int) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(targetAttendance = target))
            _events.emit("Target updated for ${subject.name}")
        }
    }

    fun updateSubjectName(subject: SubjectEntity, newName: String) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(name = newName))
            _events.emit("Subject renamed to $newName")
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            _events.emit("${subject.name} deleted")
        }
    }

    fun exportData(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            onJsonReady(json)
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            try {
                if (json.isBlank()) {
                    _events.emit("Failed to import data: Selected file is empty")
                    return@launch
                }
                repository.importDataFromJson(json)
                _events.emit("Data imported successfully! 📥")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Invalid JSON format"
                _events.emit("Failed to import data: $errorMsg")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _events.emit("All data has been reset ♻️")
        }
    }
}
