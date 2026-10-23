package com.example.attendly.data.model

data class SubjectAttendanceStats(
    val subjectId: Long,
    val attendedCount: Int,
    val absentCount: Int,
    val cancelledCount: Int
)
