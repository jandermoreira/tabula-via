package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringCalculatorTest {

    private val student = Student(studentId = "s1", name = "Test Student")
    private val classId = "c1"

    @Test
    fun `should correctly identify critical status for high absence rate`() {
        // 60% attendance = 40% absence rate.
        // Threshold for critical is 20% absence.
        
        val sessions = (1..5).map { ClassSession(sessionId = "sess$it", classId = classId, timestamp = it.toLong()) }
        val attendance = listOf(
            AttendanceRecord(sessionId = "sess1", studentId = "s1", status = AttendanceStatus.PRESENT),
            AttendanceRecord(sessionId = "sess2", studentId = "s1", status = AttendanceStatus.PRESENT),
            AttendanceRecord(sessionId = "sess3", studentId = "s1", status = AttendanceStatus.PRESENT),
            AttendanceRecord(sessionId = "sess4", studentId = "s1", status = AttendanceStatus.ABSENT),
            AttendanceRecord(sessionId = "sess5", studentId = "s1", status = AttendanceStatus.ABSENT)
        )
        // Actually this is 3/5 = 60% present, 2/5 = 40% absent.
        
        val evidences = listOf(
            Evidence(evidenceId = "e1", classId = classId, name = "ME1", type = EvidenceType.MONITORING, deadline = 100L)
        )
        val scores = listOf(
            EvidenceScore(evidenceId = "e1", studentId = "s1", score = 7.0)
        )

        val result = MonitoringCalculator.calculate(student, classId, evidences, scores, sessions, attendance)
        
        assertEquals(40.0, result.attendance, 0.01)
        // According to monitoring.md, A >= 20% is CRITICAL.
        assertEquals(MonitoringState.CRITICAL, result.state)
    }

    @Test
    fun `should not count future evidences as missing submissions`() {
        // Current time is simulated by the fact that we pass all evidences.
        // If an evidence has a deadline in the future, it shouldn't count as "missing".
        
        val currentTime = 500L
        val pastEvidence = Evidence(evidenceId = "past", classId = classId, name = "Past ME", type = EvidenceType.MONITORING, deadline = 100L)
        val futureEvidence = Evidence(evidenceId = "future", classId = classId, name = "Future ME", type = EvidenceType.MONITORING, deadline = 1000L)
        
        val evidences = listOf(pastEvidence, futureEvidence)
        
        // Student submitted the past one
        val scores = listOf(
            EvidenceScore(evidenceId = "past", studentId = "s1", score = 8.0)
        )
        
        // No attendance issues
        val sessions = emptyList<ClassSession>()
        val attendance = emptyList<AttendanceRecord>()

        val result = MonitoringCalculator.calculate(student, classId, evidences, scores, sessions, attendance)
        
        // BUG: Currently it will count 'future' as missing because it's in the list but not in scores.
        // It SHOULD be 0 missing.
        assertEquals("Should have 0 missing submissions because second one is in the future", 0, result.regularity)
        assertEquals(MonitoringState.ON_TRACK, result.state)
    }
    
    @Test
    fun `should trigger critical status for performance below 4`() {
        val evidence = Evidence(evidenceId = "e1", classId = classId, name = "ME1", type = EvidenceType.MONITORING, deadline = 100L)
        val score = EvidenceScore(evidenceId = "e1", studentId = "s1", score = 3.5)
        
        val result = MonitoringCalculator.calculate(
            student, classId, listOf(evidence), listOf(score), emptyList(), emptyList()
        )
        
        // According to monitoring.md: Pm < 4.0 is Critical.
        assertEquals(MonitoringState.CRITICAL, result.state)
    }
}
