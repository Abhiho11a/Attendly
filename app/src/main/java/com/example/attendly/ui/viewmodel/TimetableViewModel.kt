package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.entity.TimetableClassEntity
import com.example.attendly.data.repository.AttendlyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class TimetableUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val timetableByDay: Map<DayOfWeek, List<TimetableClassEntity>> = emptyMap(),
    val selectedDay: DayOfWeek = LocalDate.now().dayOfWeek,
    val isLoading: Boolean = false
)

class TimetableViewModel(private val repository: AttendlyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadTimetableData()
    }

    private fun loadTimetableData() {
        combine(
            repository.allSubjects.distinctUntilChanged(),
            repository.allTimetableClasses.distinctUntilChanged()
        ) { subjects, classes ->
            TimetableUiState(
                subjects = subjects,
                timetableByDay = classes.groupBy { it.dayOfWeek },
                selectedDay = _uiState.value.selectedDay,
                isLoading = false
            )
        }
        .flowOn(Dispatchers.Default)
        .conflate()
        .onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun selectDay(day: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun addSubject(name: String) {
        viewModelScope.launch {
            repository.insertSubject(SubjectEntity(name = name))
        }
    }

    fun addClass(
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?
    ) {
        viewModelScope.launch {
            repository.insertTimetableClass(
                TimetableClassEntity(
                    subjectId = subjectId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = room
                )
            )
            _events.emit("Class added to timetable ✨")
        }
    }

    fun updateClass(
        classId: Long,
        subjectId: Long,
        dayOfWeek: DayOfWeek,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?
    ) {
        viewModelScope.launch {
            repository.updateTimetableClass(
                TimetableClassEntity(
                    id = classId,
                    subjectId = subjectId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = room
                )
            )
            _events.emit("Class updated successfully 📝")
        }
    }

    fun deleteClass(timetableClass: TimetableClassEntity) {
        viewModelScope.launch {
            repository.deleteTimetableClass(timetableClass)
            _events.emit("Class deleted")
        }
    }
}
