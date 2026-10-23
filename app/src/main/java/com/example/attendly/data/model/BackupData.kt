package com.example.attendly.data.model

import com.example.attendly.data.entity.AttendanceRecordEntity
import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.entity.TimetableClassEntity

data class BackupData(
    val subjects: List<SubjectEntity>? = emptyList(),
    val timetable: List<TimetableClassEntity>? = emptyList(),
    val attendance: List<AttendanceRecordEntity>? = emptyList()
)
