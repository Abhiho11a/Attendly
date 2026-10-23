package com.example.attendly.data.repository

import android.content.Context
import com.example.attendly.ai.ExtractedClass
import com.example.attendly.data.dao.AttendlyDao
import com.example.attendly.data.database.DayOfWeekAdapter
import com.example.attendly.data.database.LocalDateAdapter
import com.example.attendly.data.database.LocalTimeAdapter
import com.example.attendly.data.entity.*
import com.example.attendly.data.model.BackupData
import com.example.attendly.notifications.AttendanceReminderScheduler
import com.example.attendly.widget.WidgetUpdateWorker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.glance.appwidget.updateAll
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AttendlyRepository(
    private val attendlyDao: AttendlyDao,
    private val context: Context? = null
) {

    private fun notifyWidgetUpdate() {
        context?.let { appContext ->
            WidgetUpdateWorker.updateNow(appContext)
            AttendanceReminderScheduler.schedule(appContext)
            
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    com.example.attendly.widget.AttendlyWidget().updateAll(appContext)
                } catch (_: Exception) {
                }
            }
        }
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
        .registerTypeAdapter(DayOfWeek::class.java, DayOfWeekAdapter())
        .create()

    // Subjects
    val allSubjects: Flow<List<SubjectEntity>> = attendlyDao.getAllSubjects()

    suspend fun insertSubject(subject: SubjectEntity): Long {
        val id = attendlyDao.insertSubject(subject)
        notifyWidgetUpdate()
        return id
    }

    suspend fun getSubjectByName(name: String): SubjectEntity? {
        return attendlyDao.getSubjectByName(name)
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        attendlyDao.updateSubject(subject)
        notifyWidgetUpdate()
    }

    suspend fun addGraceClass(subjectId: Long) {
        attendlyDao.incrementGraceCount(subjectId)
        notifyWidgetUpdate()
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        attendlyDao.deleteSubject(subject)
        notifyWidgetUpdate()
    }

    // Timetable
    val allTimetableClasses: Flow<List<TimetableClassEntity>> = attendlyDao.getAllTimetableClasses()

    fun getTimetableClassesForDay(dayOfWeek: DayOfWeek): Flow<List<TimetableClassEntity>> {
        return attendlyDao.getTimetableClassesForDay(dayOfWeek)
    }

    suspend fun insertTimetableClass(timetableClass: TimetableClassEntity) {
        attendlyDao.insertTimetableClass(timetableClass)
        notifyWidgetUpdate()
    }

    suspend fun updateTimetableClass(timetableClass: TimetableClassEntity) {
        attendlyDao.updateTimetableClass(timetableClass)
        notifyWidgetUpdate()
    }

    suspend fun deleteTimetableClass(timetableClass: TimetableClassEntity) {
        attendlyDao.deleteTimetableClass(timetableClass)
        notifyWidgetUpdate()
    }

    suspend fun deleteTimetableClassById(id: Long) {
        attendlyDao.deleteTimetableClassById(id)
        notifyWidgetUpdate()
    }

    // Attendance
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>> {
        return attendlyDao.getAttendanceForSubject(subjectId)
    }

    fun getAttendanceForDate(date: LocalDate): Flow<List<AttendanceRecordEntity>> {
        return attendlyDao.getAttendanceForDate(date)
    }

    fun getAttendanceForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceRecordEntity>> {
        return attendlyDao.getAttendanceForRange(startDate, endDate)
    }

    suspend fun markAttendance(
        recordId: Long = 0,
        timetableClassId: Long?,
        subjectId: Long,
        date: LocalDate,
        status: AttendanceStatus
    ) {
        val record = AttendanceRecordEntity(
            id = recordId,
            timetableClassId = timetableClassId,
            subjectId = subjectId,
            date = date,
            status = status
        )
        // If it's a timetable class and we don't have a recordId, check for existing by (classId, date)
        if (recordId == 0L && timetableClassId != null) {
            val existing = attendlyDao.getAttendanceRecord(timetableClassId, date)
            if (existing != null) {
                attendlyDao.insertOrUpdateAttendance(record.copy(id = existing.id))
                notifyWidgetUpdate()
                return
            }
        }
        
        attendlyDao.insertOrUpdateAttendance(record)
        notifyWidgetUpdate()
    }
    
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>> = attendlyDao.getAllAttendanceRecords()

    fun getSubjectAttendanceStats(): Flow<List<com.example.attendly.data.model.SubjectAttendanceStats>> = 
        attendlyDao.getSubjectAttendanceStats()

    suspend fun clearAllData() {
        attendlyDao.clearAttendance()
        attendlyDao.clearTimetable()
        attendlyDao.clearSubjects()
        notifyWidgetUpdate()
    }

    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            subjects = attendlyDao.getAllSubjectsList(),
            timetable = attendlyDao.getAllTimetableClassesList(),
            attendance = attendlyDao.getAllAttendanceRecordsList()
        )
        gson.toJson(backup)
    }

    suspend fun importDataFromJson(json: String) = withContext(Dispatchers.IO) {
        val backup = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Format error: ${e.localizedMessage}")
        }
        if (backup == null || (backup.subjects == null && backup.timetable == null && backup.attendance == null)) {
            throw IllegalArgumentException("Selected file is not a valid Attendly backup.")
        }
        attendlyDao.restoreBackup(
            backup.subjects ?: emptyList(),
            backup.timetable ?: emptyList(),
            backup.attendance ?: emptyList()
        )
        notifyWidgetUpdate()
    }

    suspend fun importClasses(extractedClasses: List<ExtractedClass>) = withContext(Dispatchers.IO) {
        val allSubjects = attendlyDao.getAllSubjectsList()
        val subjectMap = allSubjects.associateBy { it.name.uppercase() }.toMutableMap()

        extractedClasses.forEach { extracted ->
            // 1. Find or create subject
            val subjectNameKey = extracted.subject.uppercase()
            var subjectId = subjectMap[subjectNameKey]?.id
            
            if (subjectId == null) {
                val newSubject = SubjectEntity(name = extracted.subject)
                subjectId = attendlyDao.insertSubject(newSubject)
                // Update map for next iterations
                subjectMap[subjectNameKey] = newSubject.copy(id = subjectId)
            }

            // 2. Insert class (filtering should be done in ViewModel/UI)
            attendlyDao.insertTimetableClass(
                TimetableClassEntity(
                    subjectId = subjectId,
                    dayOfWeek = extracted.day,
                    startTime = extracted.startTime,
                    endTime = extracted.endTime,
                    room = extracted.room
                )
            )
        }
        notifyWidgetUpdate()
    }
}
