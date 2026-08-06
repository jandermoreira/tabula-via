package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.StudentMonitoringSummary

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
     * @param studentId The unique identifier for the student.
     * @param classId The unique identifier for the academic class.
     * @param evidences All evidences defined for the class, used to identify learning cycles.
     * @param scores All scores recorded for the student across different evidences.
     * @param sessions All class sessions held to date for the given class.
     * @param attendance The student's individual attendance records.
     * @return A [StudentMonitoringSummary] containing the derived metrics and operational status.
     */
    fun calculate(
        studentId: String,
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

        val studentAttendanceRecords = attendance.filter { it.studentId == studentId }
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
            studentId = studentId,
            classId = classId,
            regularity = missingSubmissionsCount,
            performance = monitoringPerformance,
            attendance = absenceRate,
            discrepancy = performanceDiscrepancy,
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
        val performanceTrend = mutableListOf<Double>()
        val gradesAccumulator = mutableListOf<Double>()

        for (evidence in activeMonitoringEvidences) {
            scoreLookup[evidence.evidenceId]?.score?.let { grade ->
                gradesAccumulator.add(grade)
                performanceTrend.add(gradesAccumulator.average())
            }
        }

        val currentPm = performanceTrend.lastOrNull() ?: 10.0
        val previousPm = if (performanceTrend.size >= 2) performanceTrend[performanceTrend.size - 2] else 10.0

        val persistentLowPerformance = currentPm < MINIMUM_PASSING_GRADE && previousPm < MINIMUM_PASSING_GRADE
        val criticalAttendance = absenceRate >= ATTENDANCE_CRITICAL_THRESHOLD
        
        if (missingSubmissionsCount >= 2 || persistentLowPerformance || criticalAttendance) {
            return MonitoringState.CRITICAL
        }

        val lowPerformanceSignal = currentPm < MINIMUM_PASSING_GRADE
        val attentionAttendance = absenceRate >= ATTENDANCE_ATTENTION_THRESHOLD && absenceRate < ATTENDANCE_CRITICAL_THRESHOLD

        if (missingSubmissionsCount == 1 || lowPerformanceSignal || attentionAttendance) {
            return MonitoringState.ATTENTION
        }

        return MonitoringState.ON_TRACK
    }
}
