package com.example.attendly.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.ai.ExtractedClass
import com.example.attendly.ai.TimetableAIService
import com.example.attendly.data.repository.AttendlyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream

data class ImportUiState(
    val step: ImportStep = ImportStep.SELECT_IMAGE,
    val imageUri: Uri? = null,
    val extractedClasses: List<ReviewableClass> = emptyList(),
    val isAnalyzing: Boolean = false,
    val analyzingProgress: Int = 0, // 0: Uploaded, 1: Reading, 2: Detecting, 3: Preparing
    val error: String? = null,
    val importCount: Int = 0
)

data class ReviewableClass(
    val extracted: ExtractedClass,
    val isSelected: Boolean = true,
    val isDuplicate: Boolean = false,
    val duplicateClassId: Long? = null,
    val action: ImportAction = ImportAction.ADD
)

enum class ImportAction {
    ADD,
    REPLACE,
    KEEP_EXISTING
}

enum class ImportStep {
    SELECT_IMAGE,
    PREVIEW_IMAGE,
    ANALYZING,
    REVIEWING,
    COMPLETED
}

class ImportViewModel(
    private val repository: AttendlyRepository,
    private val aiService: TimetableAIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, step = ImportStep.PREVIEW_IMAGE, error = null) }
    }

    fun analyzeTimetable(context: Context) {
        val apiKey = com.example.attendly.BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(error = "API Key is missing in local.properties") }
            return
        }

        if (_uiState.value.isAnalyzing) return

        val uri = _uiState.value.imageUri ?: return
        _uiState.update { it.copy(step = ImportStep.ANALYZING, isAnalyzing = true, analyzingProgress = 0, error = null) }

        viewModelScope.launch {
            try {
                // Step 0: Image Uploaded (Done)
                kotlinx.coroutines.delay(800)
                _uiState.update { it.copy(analyzingProgress = 1) } // Reading timetable

                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Failed to read image")
                inputStream.close()

                _uiState.update { it.copy(analyzingProgress = 2) } // Detecting classes
                val result = aiService.analyzeTimetable(bytes, apiKey)
                
                if (result.error != null) {
                    _uiState.update { it.copy(step = ImportStep.PREVIEW_IMAGE, isAnalyzing = false, error = result.error) }
                } else {
                    _uiState.update { it.copy(analyzingProgress = 3) } // Preparing schedule
                    
                    val existingClasses = repository.allTimetableClasses.first()
                    val existingSubjects = repository.allSubjects.first()
                    
                    val reviewableClasses = result.classes.map { extracted ->
                        val subjectId = existingSubjects.find { it.name.equals(extracted.subject, ignoreCase = true) }?.id
                        val existingClass = if (subjectId != null) {
                            existingClasses.find { existing ->
                                existing.subjectId == subjectId &&
                                existing.dayOfWeek == extracted.day &&
                                existing.startTime == extracted.startTime &&
                                existing.endTime == extracted.endTime
                            }
                        } else null
                        
                        ReviewableClass(
                            extracted = extracted,
                            isSelected = existingClass == null,
                            isDuplicate = existingClass != null,
                            duplicateClassId = existingClass?.id,
                            action = if (existingClass != null) ImportAction.KEEP_EXISTING else ImportAction.ADD
                        )
                    }
                    
                    kotlinx.coroutines.delay(1000)
                    _uiState.update {
                        it.copy(
                            step = ImportStep.REVIEWING,
                            isAnalyzing = false,
                            extractedClasses = reviewableClasses
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(step = ImportStep.PREVIEW_IMAGE, isAnalyzing = false, error = e.localizedMessage) }
            }
        }
    }

    fun setImportAction(index: Int, action: ImportAction) {
        _uiState.update { state ->
            val newList = state.extractedClasses.toMutableList()
            newList[index] = newList[index].copy(
                action = action,
                isSelected = action != ImportAction.KEEP_EXISTING
            )
            state.copy(extractedClasses = newList)
        }
    }

    fun updateClass(index: Int, updatedClass: ExtractedClass) {
        _uiState.update { state ->
            val newList = state.extractedClasses.toMutableList()
            newList[index] = newList[index].copy(extracted = updatedClass)
            state.copy(extractedClasses = newList)
        }
    }

    fun removeClass(index: Int) {
        _uiState.update { state ->
            val newList = state.extractedClasses.toMutableList()
            newList.removeAt(index)
            state.copy(extractedClasses = newList)
        }
    }

    fun importConfirmedClasses() {
        val reviewableList = _uiState.value.extractedClasses
        val toAdd = reviewableList.filter { it.action == ImportAction.ADD || it.action == ImportAction.REPLACE }.map { it.extracted }
        val toReplaceIds = reviewableList.filter { it.action == ImportAction.REPLACE }.mapNotNull { it.duplicateClassId }

        if (toAdd.isEmpty() && toReplaceIds.isEmpty()) return

        viewModelScope.launch {
            // 1. Delete ones to be replaced
            toReplaceIds.forEach { repository.deleteTimetableClassById(it) }
            
            // 2. Add new ones
            repository.importClasses(toAdd)
            
            _uiState.update { it.copy(step = ImportStep.COMPLETED, importCount = toAdd.size) }
        }
    }

    fun reset() {
        _uiState.value = ImportUiState()
    }
}
