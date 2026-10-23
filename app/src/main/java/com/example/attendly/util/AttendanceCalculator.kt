package com.example.attendly.util

import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.data.model.SubjectAttendanceStats
import java.util.Locale

data class CalculatedAttendance(
    val attended: Int,
    val absent: Int,
    val cancelled: Int,
    val conducted: Int,
    val percentage: Float?,
    val percentageText: String
)

object AttendanceCalculator {

    fun calculate(
        subject: SubjectEntity,
        recordAttendedCount: Int,
        recordAbsentCount: Int,
        recordCancelledCount: Int
    ): CalculatedAttendance {
        val recordConductedCount = recordAttendedCount + recordAbsentCount

        val (finalAttended, finalConducted, finalAbsent) = if (
            subject.manualAttendedCount != null && subject.manualConductedCount != null
        ) {
            val baseAttended = subject.manualAttendedCount
            val baseConducted = subject.manualConductedCount

            val baseAttendedRecord = subject.manualAttendedRecordCount ?: 0
            val baseConductedRecord = subject.manualConductedRecordCount ?: 0
            val baseGrace = subject.manualGraceCount ?: 0

            val deltaAttended = recordAttendedCount - baseAttendedRecord
            val deltaConducted = recordConductedCount - baseConductedRecord
            val deltaGrace = subject.graceCount - baseGrace

            val rawAttended = (baseAttended + deltaAttended + deltaGrace).coerceAtLeast(0)
            val rawConducted = (baseConducted + deltaConducted).coerceAtLeast(0)

            val effectiveConducted = maxOf(rawConducted, rawAttended)
            val effectiveAttended = minOf(rawAttended, effectiveConducted)
            val effectiveAbsent = (effectiveConducted - effectiveAttended).coerceAtLeast(0)

            Triple(effectiveAttended, effectiveConducted, effectiveAbsent)
        } else {
            val rawAttended = (recordAttendedCount + subject.graceCount).coerceAtLeast(0)
            val rawConducted = recordConductedCount.coerceAtLeast(0)

            val effectiveConducted = maxOf(rawConducted, rawAttended)
            val effectiveAttended = minOf(rawAttended, effectiveConducted)
            val effectiveAbsent = (effectiveConducted - effectiveAttended).coerceAtLeast(0)

            Triple(effectiveAttended, effectiveConducted, effectiveAbsent)
        }

        val percentage = if (finalConducted > 0) {
            ((finalAttended.toFloat() / finalConducted) * 100f).coerceIn(0f, 100f)
        } else {
            null
        }

        val percentageText = percentage?.let {
            String.format(Locale.getDefault(), "%.0f%%", it)
        } ?: "—"

        return CalculatedAttendance(
            attended = finalAttended.coerceAtLeast(0),
            absent = finalAbsent.coerceAtLeast(0),
            cancelled = recordCancelledCount.coerceAtLeast(0),
            conducted = finalConducted.coerceAtLeast(0),
            percentage = percentage,
            percentageText = percentageText
        )
    }

    fun calculateFromStats(
        subject: SubjectEntity,
        stat: SubjectAttendanceStats?
    ): CalculatedAttendance {
        val recordAttended = stat?.attendedCount ?: 0
        val recordAbsent = stat?.absentCount ?: 0
        val recordCancelled = stat?.cancelledCount ?: 0

        return calculate(
            subject = subject,
            recordAttendedCount = recordAttended,
            recordAbsentCount = recordAbsent,
            recordCancelledCount = recordCancelled
        )
    }
}
