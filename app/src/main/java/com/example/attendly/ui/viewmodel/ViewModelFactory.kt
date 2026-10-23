package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.attendly.ai.GeminiTimetableAIService
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.data.repository.UserPreferencesRepository

class ViewModelFactory(
    private val repository: AttendlyRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(preferencesRepository ?: throw IllegalArgumentException("Preferences required")) as T
        }
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, preferencesRepository) as T
        }
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimetableViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SubjectDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubjectDetailsViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, preferencesRepository) as T
        }
        if (modelClass.isAssignableFrom(ImportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImportViewModel(repository, GeminiTimetableAIService()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
