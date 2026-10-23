package com.example.attendly.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAttendance: Int = 75,
    val graceCount: Int = 0,
    val manualAttendedCount: Int? = null,
    val manualConductedCount: Int? = null,
    val manualAttendedRecordCount: Int? = null,
    val manualConductedRecordCount: Int? = null,
    val manualGraceCount: Int? = null
)
