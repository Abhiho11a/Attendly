package com.example.attendly.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalTime

@Entity(
    tableName = "timetable_classes",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class TimetableClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null
)
