package com.example.attendly.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class AttendanceStatus {
    PENDING,
    ATTENDED,
    ABSENT,
    CANCELLED
}

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = TimetableClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["timetableClassId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["timetableClassId", "date"]), // Removed unique to allow extra classes (which have ID 0 or null)
        Index(value = ["subjectId"])
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timetableClassId: Long?, // Nullable for extra classes
    val subjectId: Long,
    val date: LocalDate,
    val status: AttendanceStatus
)
