package com.example.attendly.data.database

import androidx.room.TypeConverter
import com.example.attendly.data.entity.AttendanceStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        try {
            LocalDate.parse(it, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let {
        try {
            LocalTime.parse(it, timeFormatter)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): Int? = value?.value

    @TypeConverter
    fun toDayOfWeek(value: Int?): DayOfWeek? = value?.let {
        try {
            DayOfWeek.of(it)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus?): String? = value?.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? = value?.let {
        try {
            AttendanceStatus.valueOf(it)
        } catch (e: Exception) {
            AttendanceStatus.PENDING
        }
    }
}
