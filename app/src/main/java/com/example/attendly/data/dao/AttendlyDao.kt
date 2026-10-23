package com.example.attendly.data.dao

import androidx.room.*
import com.example.attendly.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

@Dao
interface AttendlyDao {

    // Subjects
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE name = :name")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("UPDATE subjects SET graceCount = graceCount + 1 WHERE id = :subjectId")
    suspend fun incrementGraceCount(subjectId: Long)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    // Timetable
    @Insert
    suspend fun insertTimetableClass(timetableClass: TimetableClassEntity)

    @Update
    suspend fun updateTimetableClass(timetableClass: TimetableClassEntity)

    @Delete
    suspend fun deleteTimetableClass(timetableClass: TimetableClassEntity)

    @Query("DELETE FROM timetable_classes WHERE id = :id")
    suspend fun deleteTimetableClassById(id: Long)

    @Query("SELECT * FROM timetable_classes WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getTimetableClassesForDay(dayOfWeek: DayOfWeek): Flow<List<TimetableClassEntity>>

    @Query("SELECT * FROM timetable_classes ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllTimetableClasses(): Flow<List<TimetableClassEntity>>

    // Attendance
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecordEntity)

    @Query("SELECT * FROM attendance_records WHERE timetableClassId = :timetableClassId AND date = :date")
    suspend fun getAttendanceRecord(timetableClassId: Long, date: LocalDate): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: LocalDate): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate")
    fun getAttendanceForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("""
        SELECT 
            subjectId,
            COUNT(CASE WHEN status = 'ATTENDED' THEN 1 END) as attendedCount,
            COUNT(CASE WHEN status = 'ABSENT' THEN 1 END) as absentCount,
            COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelledCount
        FROM attendance_records
        GROUP BY subjectId
    """)
    fun getSubjectAttendanceStats(): Flow<List<com.example.attendly.data.model.SubjectAttendanceStats>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsList(): List<SubjectEntity>

    @Query("SELECT * FROM timetable_classes")
    suspend fun getAllTimetableClassesList(): List<TimetableClassEntity>

    @Query("SELECT * FROM attendance_records")
    suspend fun getAllAttendanceRecordsList(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableClasses(timetableClasses: List<TimetableClassEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)

    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM timetable_classes")
    suspend fun clearTimetable()

    @Query("DELETE FROM attendance_records")
    suspend fun clearAttendance()

    @Transaction
    suspend fun restoreBackup(
        subjects: List<SubjectEntity>,
        timetable: List<TimetableClassEntity>,
        attendance: List<AttendanceRecordEntity>
    ) {
        clearAttendance()
        clearTimetable()
        clearSubjects()
        
        insertSubjects(subjects)
        insertTimetableClasses(timetable)
        insertAttendanceRecords(attendance)
    }
}
