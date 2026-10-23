package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.entity.AttendanceRecordEntity
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.util.AttendanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class SubjectDetailsUiState(
    val subject: SubjectEntity? = null,
    val history: List<AttendanceRecordEntity> = emptyList(),
    val attendedCount: Int = 0,
    val absentCount: Int = 0,
    val cancelledCount: Int = 0,
    val conductedCount: Int = 0,
    val attendancePercentage: Float? = null,
    val isLoading: Boolean = true
)

private data class StatsResult(
    val records: List<AttendanceRecordEntity>,
    val attended: Int,
    val absent: Int,
    val cancelled: Int,
    val conducted: Int,
    val percentage: Float?
)

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectDetailsViewModel(private val repository: AttendlyRepository) : ViewModel() {

    private val _subjectId = MutableStateFlow<Long?>(null)

    private val _subjectFlow = _subjectId.filterNotNull().flatMapLatest { id ->
        repository.allSubjects.map { list -> list.find { it.id == id } }.distinctUntilChanged()
    }.flowOn(Dispatchers.IO)

    private val _historyFlow = _subjectId.filterNotNull().flatMapLatest { id ->
        repository.getAttendanceForSubject(id).distinctUntilChanged().map { records ->
            var attended = 0
            var absent = 0
            var cancelled = 0
            records.forEach { record ->
                when (record.status) {
                    AttendanceStatus.ATTENDED -> attended++
                    AttendanceStatus.ABSENT -> absent++
                    AttendanceStatus.CANCELLED -> cancelled++
                    else -> {}
                }
            }
            val conducted = attended + absent
            val percentage = if (conducted > 0) (attended.toFloat() / conducted) * 100 else null
            
            StatsResult(records, attended, absent, cancelled, conducted, percentage)
        }.flowOn(Dispatchers.Default)
    }

    val uiState: StateFlow<SubjectDetailsUiState> = combine(_subjectFlow, _historyFlow) { subject, res ->
        val calc = if (subject != null) {
            AttendanceCalculator.calculate(subject, res.attended, res.absent, res.cancelled)
        } else null

        SubjectDetailsUiState(
            subject = subject,
            history = res.records,
            attendedCount = calc?.attended ?: res.attended,
            absentCount = calc?.absent ?: res.absent,
            cancelledCount = calc?.cancelled ?: res.cancelled,
            conductedCount = calc?.conducted ?: res.conducted,
            attendancePercentage = calc?.percentage ?: res.percentage,
            isLoading = false
        )
    }
    .conflate()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectDetailsUiState(isLoading = true)
    )

    fun loadSubjectDetails(subjectId: Long) {
        _subjectId.value = subjectId
    }
}
