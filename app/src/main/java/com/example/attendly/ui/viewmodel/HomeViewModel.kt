package com.example.attendly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendly.data.entity.AttendanceStatus
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.entity.TimetableClassEntity
import com.example.attendly.data.repository.AttendlyRepository
import com.example.attendly.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.*

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDayItem: DayAttendanceItem? = null,
    val markedClassesToday: Int = 0,
    val totalClassesToday: Int = 0,
    val greeting: String = "",
    val isLoading: Boolean = true,
    val subjects: List<SubjectEntity> = emptyList(),
    val canNavigatePrevious: Boolean = true,
    val canNavigateNext: Boolean = false
)

private data class HomeDataCombine(
    val date: LocalDate,
    val subjects: List<SubjectEntity>,
    val allTimetable: List<TimetableClassEntity>,
    val progress: Pair<Int, Int>,
    val displayName: String?
)

data class DayAttendanceItem(
    val date: LocalDate,
    val dateText: String,
    val isToday: Boolean,
    val classes: List<HomeClassItem>
)

data class HomeClassItem(
    val id: Long, 
    val timetableClassId: Long?,
    val subject: SubjectEntity,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val room: String?,
    val status: AttendanceStatus,
    val isExtra: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: AttendlyRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _displayNameFlow = preferencesRepository?.displayNameFlow ?: flowOf(null)

    // Flow for Today's progress (alwaysToday)
    private val _todayProgressFlow = combine(
        repository.getTimetableClassesForDay(LocalDate.now().dayOfWeek).distinctUntilChanged(),
        repository.getAttendanceForDate(LocalDate.now()).distinctUntilChanged()
    ) { timetable, attendance ->
        val total = timetable.size + attendance.count { it.timetableClassId == null }
        val marked = attendance.count { it.status != AttendanceStatus.PENDING }
        marked to total
    }.flowOn(Dispatchers.IO)

    // Main UI State Flow
    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        repository.allSubjects.distinctUntilChanged(),
        repository.allTimetableClasses.distinctUntilChanged(),
        _todayProgressFlow,
        _displayNameFlow
    ) { date, subjects, allTimetable, progress, displayName ->
        HomeDataCombine(date, subjects, allTimetable, progress, displayName)
    }.flatMapLatest { data ->
        val (date, subjects, allTimetable, progress, displayName) = data
        val (markedToday, totalToday) = progress
        
        repository.getAttendanceForDate(date).distinctUntilChanged().map { attendanceForDate ->
            val subjectMap = subjects.associateBy { it.id }
            val dayOfWeek = date.dayOfWeek
            val isToday = date == LocalDate.now()
            
            val timetableForDay = allTimetable.filter { it.dayOfWeek == dayOfWeek }
            val timetableAttendanceMap = attendanceForDate.filter { it.timetableClassId != null }.associateBy { it.timetableClassId!! }
            
            val timetableItems = timetableForDay.map { tClass ->
                val record = timetableAttendanceMap[tClass.id]
                HomeClassItem(
                    id = record?.id ?: tClass.id,
                    timetableClassId = tClass.id,
                    subject = subjectMap[tClass.subjectId] ?: SubjectEntity(name = "Unknown"),
                    startTime = tClass.startTime,
                    endTime = tClass.endTime,
                    room = tClass.room,
                    status = record?.status ?: AttendanceStatus.PENDING,
                    isExtra = false
                )
            }
            
            val extraItems = attendanceForDate.filter { it.timetableClassId == null }.map { record ->
                HomeClassItem(
                    id = record.id,
                    timetableClassId = null,
                    subject = subjectMap[record.subjectId] ?: SubjectEntity(name = "Unknown"),
                    startTime = null,
                    endTime = null,
                    room = null,
                    status = record.status,
                    isExtra = true
                )
            }
            
            val today = LocalDate.now()
            val minDate = today.minusDays(365)
            val canPrev = date.isAfter(minDate)
            val canNext = date.isBefore(today)

            val dateText = when (date) {
                today -> "Today, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                today.minusDays(1) -> "Yesterday, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                else -> "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}, ${date.year}"
            }
            
            val greeting = calculateGreeting(displayName)

            HomeUiState(
                selectedDate = date,
                selectedDayItem = DayAttendanceItem(
                    date = date,
                    dateText = dateText,
                    isToday = isToday,
                    classes = (timetableItems + extraItems).sortedBy { it.startTime ?: LocalTime.MAX }
                ),
                markedClassesToday = markedToday,
                totalClassesToday = totalToday,
                greeting = greeting,
                subjects = subjects,
                isLoading = false,
                canNavigatePrevious = canPrev,
                canNavigateNext = canNext
            )
        }
    }
    .flowOn(Dispatchers.Default)
    .conflate()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    private fun calculateGreeting(name: String?): String {
        val hour = LocalTime.now().hour
        val timePeriod = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Good night"
        }
        val cleanName = name?.trim()
        return if (!cleanName.isNullOrBlank()) {
            "$timePeriod, $cleanName 👋"
        } else {
            "$timePeriod 👋"
        }
    }

    fun selectPreviousDay() {
        _selectedDate.update { current ->
            val minDate = LocalDate.now().minusDays(365)
            val prev = current.minusDays(1)
            if (prev.isBefore(minDate)) minDate else prev
        }
    }

    fun addSubject(name: String) {
        viewModelScope.launch {
            repository.insertSubject(SubjectEntity(name = name))
        }
    }

    fun selectNextDay() {
        _selectedDate.update { current ->
            val today = LocalDate.now()
            val next = current.plusDays(1)
            if (next.isAfter(today)) today else next
        }
    }

    fun markAttendance(item: HomeClassItem, date: LocalDate, status: AttendanceStatus) {
        viewModelScope.launch {
            val recordId = if (item.timetableClassId != null && item.id == item.timetableClassId) 0L else item.id
            repository.markAttendance(
                recordId = recordId,
                timetableClassId = item.timetableClassId,
                subjectId = item.subject.id,
                date = date,
                status = status
            )
            val msg = when(status) {
                AttendanceStatus.ATTENDED -> "${item.subject.name} marked as Attended ✓"
                AttendanceStatus.ABSENT -> "${item.subject.name} marked as Absent ✕"
                AttendanceStatus.CANCELLED -> "${item.subject.name} cancelled —"
                else -> ""
            }
            if (msg.isNotEmpty()) _events.emit(msg)
        }
    }

    fun addExtraClass(subjectId: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.markAttendance(0, null, subjectId, _selectedDate.value, status)
            _events.emit("Extra class logged ✨")
        }
    }

    fun cancelAllClassesForSelectedDate() {
        viewModelScope.launch {
            val dayItem = uiState.value.selectedDayItem ?: return@launch
            val date = dayItem.date
            val classes = dayItem.classes
            if (classes.isEmpty()) return@launch

            classes.forEach { item ->
                val recordId = if (item.timetableClassId != null && item.id == item.timetableClassId) 0L else item.id
                repository.markAttendance(
                    recordId = recordId,
                    timetableClassId = item.timetableClassId,
                    subjectId = item.subject.id,
                    date = date,
                    status = AttendanceStatus.CANCELLED
                )
            }
            val dateLabel = if (dayItem.isToday) "today's" else "selected day's"
            _events.emit("All $dateLabel classes marked as Cancelled —")
        }
    }
}
