package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        if (newName.length <= 50) {
            _uiState.update { it.copy(name = newName, error = null) }
        }
    }

    fun submitName(onSuccess: () -> Unit) {
        val trimmed = _uiState.value.name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your name") }
            return
        }

        if (_uiState.value.isProcessing) return

        _uiState.update { it.copy(isProcessing = true, error = null) }

        viewModelScope.launch {
            val result = preferencesRepository.registerUser(trimmed)
            if (result.isSuccess) {
                _uiState.update { it.copy(isProcessing = false) }
                onSuccess()
            } else {
                _uiState.update { 
                    it.copy(
                        isProcessing = false, 
                        error = "Unable to complete setup. Please check your internet connection and try again."
                    ) 
                }
            }
        }
    }
}
