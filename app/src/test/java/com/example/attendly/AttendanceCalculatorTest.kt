package com.example.attendly

import com.example.attendly.data.entity.SubjectEntity
import com.example.attendly.util.AttendanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AttendanceCalculatorTest {

    // --- REFERENCE CALCULATOR ORACLE ---
    private data class ReferenceState(
        var recordAttended: Int = 0,
        var recordAbsent: Int = 0,
        var recordCancelled: Int = 0,
        var manualAttended: Int? = null,
        var manualConducted: Int? = null,
        var manualAttendedRecord: Int? = null,
        var manualConductedRecord: Int? = null,
        var manualGrace: Int? = null,
        var grace: Int = 0
    ) {
        fun expectedAttended(): Int {
            val rawAttended = if (manualAttended != null && manualConducted != null) {
                val baseA = manualAttended!!
                val baseRecA = manualAttendedRecord ?: 0
                val baseG = manualGrace ?: 0
                val deltaA = recordAttended - baseRecA
                val deltaG = grace - baseG
                (baseA + deltaA + deltaG).coerceAtLeast(0)
            } else {
                (recordAttended + grace).coerceAtLeast(0)
            }
            val rawConducted = expectedConductedRaw()
            val effectiveConducted = maxOf(rawConducted, rawAttended)
            return minOf(rawAttended, effectiveConducted)
        }

        private fun expectedConductedRaw(): Int {
            return if (manualAttended != null && manualConducted != null) {
                val baseC = manualConducted!!
                val baseRecC = manualConductedRecord ?: 0
                val recordConducted = recordAttended + recordAbsent
                val deltaC = recordConducted - baseRecC
                (baseC + deltaC).coerceAtLeast(0)
            } else {
                (recordAttended + recordAbsent).coerceAtLeast(0)
            }
        }

        fun expectedConducted(): Int {
            val rawAttended = if (manualAttended != null && manualConducted != null) {
                val baseA = manualAttended!!
                val baseRecA = manualAttendedRecord ?: 0
                val baseG = manualGrace ?: 0
                val deltaA = recordAttended - baseRecA
                val deltaG = grace - baseG
                (baseA + deltaA + deltaG).coerceAtLeast(0)
            } else {
                (recordAttended + grace).coerceAtLeast(0)
            }
            val rawConducted = expectedConductedRaw()
            return maxOf(rawConducted, rawAttended)
        }

        fun expectedAbsent(): Int {
            return (expectedConducted() - expectedAttended()).coerceAtLeast(0)
        }

        fun expectedPercentage(): Float? {
            val cond = expectedConducted()
            return if (cond > 0) {
                ((expectedAttended().toFloat() / cond) * 100f).coerceIn(0f, 100f)
            } else null
        }
    }

    // --- PROMPT REQUIRED CASE 1: Attended=3, Absent=2, Grace=0 ---
    @Test
    fun testCase1_Attended3_Absent2_Grace0() {
        val subject = SubjectEntity(name = "Math", graceCount = 0)
        val calc = AttendanceCalculator.calculate(subject, 3, 2, 0)

        assertEquals(3, calc.attended)
        assertEquals(5, calc.conducted)
        assertEquals(2, calc.absent)
        assertEquals(60.0f, calc.percentage!!, 0.01f)
        assertTrue(calc.attended <= calc.conducted)
        assertEquals(calc.conducted, calc.attended + calc.absent)
    }

    // --- PROMPT REQUIRED CASE 2: Attended=3, Absent=2, Grace=1 ---
    @Test
    fun testCase2_Attended3_Absent2_Grace1() {
        val subject = SubjectEntity(name = "Math", graceCount = 1)
        val calc = AttendanceCalculator.calculate(subject, 3, 2, 0)

        assertEquals(4, calc.attended)
        assertEquals(5, calc.conducted)
        assertEquals(1, calc.absent)
        assertEquals(80.0f, calc.percentage!!, 0.01f)
        assertTrue(calc.attended <= calc.conducted)
        assertEquals(calc.conducted, calc.attended + calc.absent)
    }

    // --- PROMPT REQUIRED CASE 3: Attended=3, Absent=2, Grace=2 ---
    @Test
    fun testCase3_Attended3_Absent2_Grace2() {
        val subject = SubjectEntity(name = "Math", graceCount = 2)
        val calc = AttendanceCalculator.calculate(subject, 3, 2, 0)

        assertEquals(5, calc.attended)
        assertEquals(5, calc.conducted)
        assertEquals(0, calc.absent)
        assertEquals(100.0f, calc.percentage!!, 0.01f)
        assertTrue(calc.attended <= calc.conducted)
        assertEquals(calc.conducted, calc.attended + calc.absent)
    }

    // --- PROMPT REQUIRED CASE 4: Attended=3, Absent=2, Grace=4 ---
    @Test
    fun testCase4_Attended3_Absent2_Grace4() {
        val subject = SubjectEntity(name = "Math", graceCount = 4)
        val calc = AttendanceCalculator.calculate(subject, 3, 2, 0)

        assertEquals(7, calc.attended)
        assertEquals(7, calc.conducted)
        assertEquals(0, calc.absent)
        assertEquals(100.0f, calc.percentage!!, 0.01f)
        assertTrue(calc.attended <= calc.conducted)
        assertEquals(calc.conducted, calc.attended + calc.absent)
    }

    // --- PROMPT REQUIRED CASE 5: Manual baseline enabled ---
    @Test
    fun testCase5_ManualBaselineCombinations() {
        val subject = SubjectEntity(
            name = "Physics",
            manualAttendedCount = 20,
            manualConductedCount = 25,
            manualAttendedRecordCount = 10,
            manualConductedRecordCount = 15,
            manualGraceCount = 1,
            graceCount = 3
        )
        val calc = AttendanceCalculator.calculate(
            subject = subject,
            recordAttendedCount = 12,
            recordAbsentCount = 5,
            recordCancelledCount = 0
        )

        // Delta recordAttended = 12 - 10 = 2
        // Delta recordConducted = 17 - 15 = 2
        // Delta grace = 3 - 1 = 2
        // rawAttended = 20 + 2 + 2 = 24
        // rawConducted = 25 + 2 = 27
        assertEquals(24, calc.attended)
        assertEquals(27, calc.conducted)
        assertEquals(3, calc.absent)
        assertTrue(calc.attended <= calc.conducted)
        assertEquals(calc.conducted, calc.attended + calc.absent)
    }

    // --- PROMPT REQUIRED CASE 6: Apply Grace then Restart App ---
    @Test
    fun testCase6_GracePersistedAndRecalculated() {
        var subject = SubjectEntity(name = "Chemistry", graceCount = 0)
        var calc1 = AttendanceCalculator.calculate(subject, 5, 5, 0)
        assertEquals(5, calc1.attended)
        assertEquals(10, calc1.conducted)

        // Grace applied -> graceCount = 2
        subject = subject.copy(graceCount = 2)
        var calc2 = AttendanceCalculator.calculate(subject, 5, 5, 0)
        assertEquals(7, calc2.attended)
        assertEquals(10, calc2.conducted)
        assertEquals(3, calc2.absent)
    }

    // --- PROMPT REQUIRED CASE 7 & 8: Apply Grace then Manual Edit & vice versa ---
    @Test
    fun testCase7And8_GraceAndManualEditInteraction() {
        var subject = SubjectEntity(
            name = "Biology",
            manualAttendedCount = 15,
            manualConductedCount = 20,
            manualAttendedRecordCount = 5,
            manualConductedRecordCount = 10,
            manualGraceCount = 1,
            graceCount = 2
        )
        var calc = AttendanceCalculator.calculate(subject, 5, 5, 0)
        assertEquals(16, calc.attended)
        assertEquals(20, calc.conducted)
        assertEquals(4, calc.absent)
    }

    // --- PROMPT REQUIRED CASE 9: Multiple Subjects Isolation ---
    @Test
    fun testCase9_MultipleSubjectsIsolated() {
        val subject1 = SubjectEntity(id = 1, name = "Subject A", graceCount = 2)
        val subject2 = SubjectEntity(id = 2, name = "Subject B", graceCount = 0)

        val calc1 = AttendanceCalculator.calculate(subject1, 4, 4, 0)
        val calc2 = AttendanceCalculator.calculate(subject2, 4, 4, 0)

        assertEquals(6, calc1.attended)
        assertEquals(8, calc1.conducted)

        assertEquals(4, calc2.attended)
        assertEquals(8, calc2.conducted)
    }

    // --- PROMPT REQUIRED CASE 10: Normal Attendance Update After Grace ---
    @Test
    fun testCase10_AttendanceUpdateAfterGrace() {
        var subject = SubjectEntity(name = "History", graceCount = 2)
        var calc = AttendanceCalculator.calculate(subject, 3, 2, 0)
        assertEquals(5, calc.attended)
        assertEquals(5, calc.conducted)

        // New class marked ATTENDED
        calc = AttendanceCalculator.calculate(subject, 4, 2, 0)
        assertEquals(6, calc.attended)
        assertEquals(6, calc.conducted)
        assertEquals(0, calc.absent)

        // New class marked ABSENT
        calc = AttendanceCalculator.calculate(subject, 4, 3, 0)
        assertEquals(6, calc.attended)
        assertEquals(7, calc.conducted)
        assertEquals(1, calc.absent)
    }

    @Test
    fun testRandomizedPropertyBasedScenarios() {
        val seed = 48291L
        val random = Random(seed)
        val numScenarios = 1000

        for (i in 0 until numScenarios) {
            val state = ReferenceState()
            var subject = SubjectEntity(name = "Subject_$i")

            val numOps = random.nextInt(20) + 1
            for (op in 0 until numOps) {
                when (random.nextInt(7)) {
                    0 -> state.recordAttended++
                    1 -> state.recordAbsent++
                    2 -> state.recordCancelled++
                    3 -> if (state.recordAttended > 0) {
                        state.recordAttended--
                        state.recordAbsent++
                    }
                    4 -> if (state.recordAbsent > 0) {
                        state.recordAbsent--
                        state.recordAttended++
                    }
                    5 -> {
                        state.grace++
                        subject = subject.copy(graceCount = state.grace)
                    }
                    6 -> {
                        val newAttended = random.nextInt(50)
                        val newConducted = newAttended + random.nextInt(20)
                        val newGrace = random.nextInt(10)

                        state.manualAttended = newAttended
                        state.manualConducted = newConducted
                        state.manualAttendedRecord = state.recordAttended
                        state.manualConductedRecord = state.recordAttended + state.recordAbsent
                        state.manualGrace = newGrace
                        state.grace = newGrace

                        subject = subject.copy(
                            manualAttendedCount = newAttended,
                            manualConductedCount = newConducted,
                            manualAttendedRecordCount = state.manualAttendedRecord,
                            manualConductedRecordCount = state.manualConductedRecord,
                            manualGraceCount = state.manualGrace,
                            graceCount = newGrace
                        )
                    }
                }

                val calc = AttendanceCalculator.calculate(
                    subject = subject,
                    recordAttendedCount = state.recordAttended,
                    recordAbsentCount = state.recordAbsent,
                    recordCancelledCount = state.recordCancelled
                )

                assertEquals("Scenario $i Op $op Attended mismatch", state.expectedAttended(), calc.attended)
                assertEquals("Scenario $i Op $op Conducted mismatch", state.expectedConducted(), calc.conducted)
                assertEquals("Scenario $i Op $op Absent mismatch", state.expectedAbsent(), calc.absent)
                
                // Assert Invariants
                assertTrue("Attended > Conducted in Scenario $i Op $op", calc.attended <= calc.conducted)
                assertEquals("Sum mismatch in Scenario $i Op $op", calc.conducted, calc.attended + calc.absent)
            }
        }
    }
}
