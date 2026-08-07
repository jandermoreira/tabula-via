package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringCalculatorTest {

    private val student = Student(studentId = "s1", name = "Test Student")
    private val classId = "c1"
    private val totalSessions = 10

    @Test
    fun `should correctly identify critical status for high absence rate`() {
        // 2 absences out of 10 planned = 20% absence rate.
        // Threshold for critical is 20% absence.
        
        val sessions = (1..2).map { ClassSession(sessionId = "sess$it", classId = classId, timestamp = it.toLong()) }
        val attendance = listOf(
            AttendanceRecord(sessionId = "sess1", studentId = "s1", status = AttendanceStatus.ABSENT),
            AttendanceRecord(sessionId = "sess2", studentId = "s1", status = AttendanceStatus.ABSENT)
        )
        
        val result = MonitoringCalculator.calculate(
            student, classId, emptyList(), emptyList(), sessions, attendance, totalSessions
        )
        
        assertEquals(20.0, result.attendance, 0.01)
        assertEquals(MonitoringState.CRITICAL, result.attendanceState)
        assertEquals(MonitoringState.CRITICAL, result.state)
    }

    @Test
    fun `should not count future evidences as missing submissions`() {
        val referenceTime = 500L
        val pastEvidence = Evidence(evidenceId = "past", classId = classId, name = "Past ME", type = EvidenceType.MONITORING, deadline = 100L)
        val futureEvidence = Evidence(evidenceId = "future", classId = classId, name = "Future ME", type = EvidenceType.MONITORING, deadline = 1000L)
        
        val evidences = listOf(pastEvidence, futureEvidence)
        val scores = listOf(EvidenceScore(evidenceId = "past", studentId = "s1", score = 8.0))

        val result = MonitoringCalculator.calculate(
            student, classId, evidences, scores, emptyList(), emptyList(), totalSessions, referenceTime
        )
        
        assertEquals(0, result.regularity)
        assertEquals(MonitoringState.ON_TRACK, result.regularityState)
        assertEquals(MonitoringState.ON_TRACK, result.state)
    }
    
    @Test
    fun `should trigger critical status for performance below 4`() {
        val evidence = Evidence(evidenceId = "e1", classId = classId, name = "ME1", type = EvidenceType.MONITORING, deadline = 100L)
        val score = EvidenceScore(evidenceId = "e1", studentId = "s1", score = 3.5)
        
        val result = MonitoringCalculator.calculate(
            student, classId, listOf(evidence), listOf(score), emptyList(), emptyList(), totalSessions
        )
        
        assertEquals(MonitoringState.CRITICAL, result.performanceState)
        assertEquals(MonitoringState.CRITICAL, result.state)
    }
}
