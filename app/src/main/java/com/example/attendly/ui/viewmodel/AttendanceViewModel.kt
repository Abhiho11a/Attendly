package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.ui.state.UiState
import com.example.attendly.util.AttendanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

enum class AttendanceViewMode {
    SUBJECT_WISE,
    DAY_WISE
}

data class AttendanceData(
    val subjectsAttendance: List<SubjectAttendanceItem> = emptyList(),
    val dayWiseAttendance: List<DayWiseAttendanceItem> = emptyList(),
    val viewMode: AttendanceViewMode = AttendanceViewMode.SUBJECT_WISE
)

data class SubjectAttendanceItem(
    val subject: SubjectEntity,
    val attendedCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val conductedCount: Int,
    val attendancePercentage: Float?,
    val percentageText: String,
    val bunkStatus: BunkStatus
)

data class DayWiseAttendanceItem(
    val date: LocalDate,
    val dateText: String,
    val records: List<DayClassStatusItem>
)

data class DayClassStatusItem(
    val recordId: Long,
    val timetableClassId: Long?,
    val subjectId: Long,
    val subjectName: String,
    val status: AttendanceStatus,
    val isExtra: Boolean = false
)

sealed class BunkStatus {
    object NoData : BunkStatus()
    data class CanBunk(val count: Int) : BunkStatus()
    data class MustAttend(val count: Int) : BunkStatus()
}

class AttendanceViewModel(private val repository: AttendlyRepository) : ViewModel() {

    private val _viewMode = MutableStateFlow(AttendanceViewMode.SUBJECT_WISE)
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val uiState: StateFlow<UiState<AttendanceData>> = combine(
        repository.allSubjects.distinctUntilChanged(),
        repository.getSubjectAttendanceStats().distinctUntilChanged(),
        repository.getAllAttendanceRecords().distinctUntilChanged(),
        _viewMode
    ) { subjects, stats, allRecords, viewMode ->
        if (subjects.isEmpty() && allRecords.isEmpty()) {
            UiState.Empty
        } else {
            val statsMap = stats.associateBy { it.subjectId }
            val subjectMap = subjects.associateBy { it.id }
            
            val items = subjects.map { subject ->
                val stat = statsMap[subject.id]
                val calc = AttendanceCalculator.calculateFromStats(subject, stat)

                val target = subject.targetAttendance
                val bunkStatus = if (calc.conducted == 0) {
                    BunkStatus.NoData
                } else if (calc.percentage != null && calc.percentage >= target) {
                    val safe = floor((100.0 * calc.attended / target) - calc.conducted).toInt()
                    BunkStatus.CanBunk(safe.coerceAtLeast(0))
                } else {
                    val required = if (target < 100) {
                        ceil((target.toDouble() * calc.conducted - 100.0 * calc.attended) / (100.0 - target)).toInt()
                    } else 999
                    BunkStatus.MustAttend(required.coerceAtLeast(1))
                }
                
                SubjectAttendanceItem(
                    subject = subject,
                    attendedCount = calc.attended,
                    absentCount = calc.absent,
                    cancelledCount = calc.cancelled,
                    conductedCount = calc.conducted,
                    attendancePercentage = calc.percentage,
                    percentageText = calc.percentageText,
                    bunkStatus = bunkStatus
                )
            }

            val recordsByDate = allRecords.groupBy { it.date }.entries.sortedByDescending { it.key }
            val dayWiseItems = recordsByDate.map { (date, records) ->
                val dateText = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}, ${date.year}"
                val classItems = records.map { record ->
                    val subj = subjectMap[record.subjectId]
                    DayClassStatusItem(
                        recordId = record.id,
                        timetableClassId = record.timetableClassId,
                        subjectId = record.subjectId,
                        subjectName = subj?.name ?: "Unknown",
                        status = record.status,
                        isExtra = record.timetableClassId == null
                    )
                }
                DayWiseAttendanceItem(
                    date = date,
                    dateText = dateText,
                    records = classItems
                )
            }

            UiState.Success(
                AttendanceData(
                    subjectsAttendance = items,
                    dayWiseAttendance = dayWiseItems,
                    viewMode = viewMode
                )
            )
        }
    }
    .flowOn(Dispatchers.IO)
    .conflate()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    fun setViewMode(mode: AttendanceViewMode) {
        _viewMode.value = mode
    }

    fun addGraceClass(subjectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = uiState.value
            if (currentState is UiState.Success) {
                val currentItem = currentState.data.subjectsAttendance.find { it.subject.id == subjectId }
                if (currentItem != null && currentItem.attendedCount + 1 > currentItem.conductedCount) {
                    _events.emit("Attended count cannot exceed total classes ⚠️")
                    return@launch
                }
            }
            repository.addGraceClass(subjectId)
            _events.emit("Grace class added successfully ✨")
        }
    }

    fun updateManualCounts(subjectId: Long, attended: Int, conducted: Int, grace: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (attended > conducted) {
                _events.emit("Attended count cannot exceed total classes ⚠️")
                return@launch
            }
            val subjects = repository.allSubjects.first()
            val subject = subjects.find { it.id == subjectId }
            if (subject != null) {
                val stat = repository.getSubjectAttendanceStats().first().find { it.subjectId == subjectId }
                val recordAttended = stat?.attendedCount ?: 0
                val recordConducted = recordAttended + (stat?.absentCount ?: 0)

                repository.updateSubject(
                    subject.copy(
                        manualAttendedCount = attended.coerceAtLeast(0),
                        manualConductedCount = conducted.coerceAtLeast(0),
                        manualAttendedRecordCount = recordAttended,
                        manualConductedRecordCount = recordConducted,
                        manualGraceCount = grace.coerceAtLeast(0),
                        graceCount = grace.coerceAtLeast(0)
                    )
                )
                _events.emit("Counts updated for ${subject.name} ✨")
            }
        }
    }

    fun updateHistoricalRecordStatus(record: DayClassStatusItem, date: LocalDate, newStatus: AttendanceStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAttendance(
                recordId = record.recordId,
                timetableClassId = record.timetableClassId,
                subjectId = record.subjectId,
                date = date,
                status = newStatus
            )
            val statusLabel = when (newStatus) {
                AttendanceStatus.ATTENDED -> "Attended ✓"
                AttendanceStatus.ABSENT -> "Absent ✕"
                AttendanceStatus.CANCELLED -> "Cancelled —"
                else -> "Pending"
            }
            _events.emit("${record.subjectName} status updated to $statusLabel ✨")
        }
    }
}
