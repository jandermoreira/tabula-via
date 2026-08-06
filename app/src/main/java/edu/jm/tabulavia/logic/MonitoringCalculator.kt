package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary

import edu.jm.tabulavia.model.Student

/**
 * Computes pedagogical monitoring indicators based on student work rhythm.
 *
 * This calculator processes evidence scores and attendance records to determine
 * a student's operational status and identifies potential risks in their
 * learning progression according to the established institutional guidelines.
 */
object MonitoringCalculator {

    private const val MINIMUM_PASSING_GRADE = 6.0
    private const val ATTENDANCE_ATTENTION_THRESHOLD = 15.0
    private const val ATTENDANCE_CRITICAL_THRESHOLD = 20.0

    /**
     * Calculates the monitoring summary for a specific student within a class.
     *
     * @param student The student object.
     * @param classId The unique identifier for the academic class.
     * @param evidences All evidences defined for the class, used to identify learning cycles.
     * @param scores All scores recorded for the student across different evidences.
     * @param sessions All class sessions held to date for the given class.
     * @param attendance The student's individual attendance records.
     * @return A [StudentMonitoringSummary] containing the derived metrics and operational status.
     */
    fun calculate(
        student: Student,
        classId: String,
        evidences: List<Evidence>,
        scores: List<EvidenceScore>,
        sessions: List<ClassSession>,
        attendance: List<AttendanceRecord>
    ): StudentMonitoringSummary {
        val chronologicalEvidences = evidences.sortedBy { it.deadline }
        val scoreLookup = scores.associateBy { it.evidenceId }

        val learningCycles = groupEvidencesIntoCycles(chronologicalEvidences)
        val activeCycle = learningCycles.lastOrNull() ?: emptyList()
        val activeMonitoringEvidences = activeCycle.filter { it.type == EvidenceType.MONITORING }

        val missingSubmissionsCount = activeMonitoringEvidences.count { !scoreLookup.containsKey(it.evidenceId) }

        val activeCycleGrades = activeMonitoringEvidences.mapNotNull { scoreLookup[it.evidenceId]?.score }
        val monitoringPerformance = if (activeCycleGrades.isNotEmpty()) activeCycleGrades.average() else null

        val studentAttendanceRecords = attendance.filter { it.studentId == student.studentId }
        val totalSessionsCount = sessions.size
        val absencesCount = studentAttendanceRecords.count { it.status == AttendanceStatus.ABSENT }
        val absenceRate = if (totalSessionsCount > 0) (absencesCount.toDouble() / totalSessionsCount) * 100.0 else 0.0

        val performanceDiscrepancy = calculateDiscrepancy(learningCycles, scoreLookup)

        val operationalStatus = evaluateOperationalStatus(
            missingSubmissionsCount = missingSubmissionsCount,
            absenceRate = absenceRate,
            activeMonitoringEvidences = activeMonitoringEvidences,
            scoreLookup = scoreLookup
        )

        return StudentMonitoringSummary(
            student = student,
            regularity = missingSubmissionsCount,
            performance = monitoringPerformance,
            attendance = absenceRate,
            discrepancy = performanceDiscrepancy,
            hasDiscrepancyFlag = performanceDiscrepancy?.let { it >= 3.0 } ?: false,
            state = operationalStatus
        )
    }

    /**
     * Groups evidences into learning cycles based on chronological order.
     *
     * A cycle is defined as a sequence of Monitoring Evidences terminated by one Consolidation Evidence.
     *
     * @param evidences Sorted list of class evidences.
     * @return A list of learning cycles, where each cycle is a list of evidences.
     */
    private fun groupEvidencesIntoCycles(evidences: List<Evidence>): List<List<Evidence>> {
        val cycles = mutableListOf<List<Evidence>>()
        var currentCycle = mutableListOf<Evidence>()

        for (evidence in evidences) {
            currentCycle.add(evidence)
            if (evidence.type == EvidenceType.CONSOLIDATION) {
                cycles.add(currentCycle)
                currentCycle = mutableListOf()
            }
        }
        if (currentCycle.isNotEmpty()) {
            cycles.add(currentCycle)
        }
        return cycles
    }

    /**
     * Calculates the discrepancy between monitoring performance and consolidation grade.
     *
     * Uses the most recently completed cycle that contains a consolidation event.
     *
     * @param cycles List of grouped learning cycles.
     * @param scoreLookup Map for student score retrieval.
     * @return The difference (Pm - CE) or null if data is insufficient.
     */
    private fun calculateDiscrepancy(
        cycles: List<List<Evidence>>,
        scoreLookup: Map<String, EvidenceScore>
    ): Double? {
        val lastCompletedCycle = cycles.lastOrNull { cycle ->
            cycle.any { it.type == EvidenceType.CONSOLIDATION }
        } ?: return null

        val consolidationEvidence = lastCompletedCycle.find { it.type == EvidenceType.CONSOLIDATION }
        val consolidationGrade = consolidationEvidence?.let { scoreLookup[it.evidenceId]?.score }
        
        val monitoringAverage = lastCompletedCycle
            .filter { it.type == EvidenceType.MONITORING }
            .mapNotNull { scoreLookup[it.evidenceId]?.score }
            .let { if (it.isNotEmpty()) it.average() else null }

        return if (consolidationGrade != null && monitoringAverage != null) {
            monitoringAverage - consolidationGrade
        } else null
    }

    /**
     * Determines the student's operational status based on trigger conditions.
     * Follows the pedagogical guidelines from monitoring.md.
     *
     * @param missingSubmissionsCount Number of missing submissions in the current cycle.
     * @param absenceRate Accumulated absence percentage.
     * @param activeMonitoringEvidences Monitoring evidences of the active cycle.
     * @param scoreLookup Map for student score retrieval.
     * @return The derived [MonitoringState].
     */
    private fun evaluateOperationalStatus(
        missingSubmissionsCount: Int,
        absenceRate: Double,
        activeMonitoringEvidences: List<Evidence>,
        scoreLookup: Map<String, EvidenceScore>
    ): MonitoringState {
        val individualGrades = activeMonitoringEvidences.mapNotNull { scoreLookup[it.evidenceId]?.score }
        
        // Critical Triggers:
        // - Two or more missing submissions
        // - Pm < 6.0 in two consecutive Monitoring Evidences
        // - Attendance risk (A >= 20%)
        val hasTwoConsecutiveLowGrades = if (individualGrades.size >= 2) {
            individualGrades.windowed(2).any { window -> 
                window.all { it < MINIMUM_PASSING_GRADE } 
            }
        } else false
        
        val criticalAttendance = absenceRate >= ATTENDANCE_CRITICAL_THRESHOLD
        val criticalRegularity = missingSubmissionsCount >= 2
        
        if (criticalRegularity || hasTwoConsecutiveLowGrades || criticalAttendance) {
            return MonitoringState.CRITICAL
        }

        // Attention Triggers:
        // - One missing submission
        // - Pm < 6.0 caused by one Monitoring Evidence
        // - Attendance attention (15% <= A < 20%)
        val hasLowPerformanceSignal = individualGrades.any { it < MINIMUM_PASSING_GRADE }
        val attentionAttendance = absenceRate >= ATTENDANCE_ATTENTION_THRESHOLD
        val attentionRegularity = missingSubmissionsCount == 1

        if (attentionRegularity || hasLowPerformanceSignal || attentionAttendance) {
            return MonitoringState.ATTENTION
        }

        return MonitoringState.ON_TRACK
    }
}
