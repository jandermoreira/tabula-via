package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.InterventionAction
import edu.jm.tabulavia.model.MonitoringState
import edu.jm.tabulavia.model.Student
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
     * Calculates the chronological history of monitoring indicators for a student.
     *
     * @param student The student object.
     * @param classId The unique identifier for the academic class.
     * @param evidences All evidences defined for the class.
     * @param scores All scores recorded for the student.
     * @param sessions All class sessions held to date.
     * @param attendance The student's individual attendance records.
     * @return A list of [EvidenceHistoryItem] representing the evolution of indicators.
     */
    fun calculateHistory(
        student: Student,
        classId: String,
        evidences: List<Evidence>,
        scores: List<EvidenceScore>,
        sessions: List<ClassSession>,
        attendance: List<AttendanceRecord>,
        totalPlannedSessions: Int,
        referenceTime: Long? = null
    ): List<edu.jm.tabulavia.model.EvidenceHistoryItem> {
        val chronologicalEvidences = evidences.sortedBy { it.deadline }
        val scoreLookup = scores.associateBy { it.evidenceId }

        return chronologicalEvidences.map { currentEvidence ->
            val evidencesUntilNow = chronologicalEvidences.filter { it.deadline <= currentEvidence.deadline }
            val scoresUntilNow = scores.filter { score -> 
                evidencesUntilNow.any { it.evidenceId == score.evidenceId }
            }
            val sessionsUntilNow = sessions.filter { it.timestamp <= currentEvidence.deadline }
            val attendanceUntilNow = attendance.filter { record ->
                sessionsUntilNow.any { it.sessionId == record.sessionId }
            }

            val snapshot = calculate(
                student = student,
                classId = classId,
                evidences = evidencesUntilNow,
                scores = scoresUntilNow,
                sessions = sessionsUntilNow,
                attendance = attendanceUntilNow,
                totalPlannedSessions = totalPlannedSessions,
                referenceTime = currentEvidence.deadline
            )

            edu.jm.tabulavia.model.EvidenceHistoryItem(
                evidenceName = currentEvidence.name,
                deadline = currentEvidence.deadline,
                score = scoreLookup[currentEvidence.evidenceId]?.score,
                type = currentEvidence.type,
                snapshot = snapshot
            )
        }
    }

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
        attendance: List<AttendanceRecord>,
        totalPlannedSessions: Int,
        referenceTime: Long? = null
    ): StudentMonitoringSummary {
        val now = referenceTime ?: System.currentTimeMillis()
        val chronologicalEvidences = evidences.sortedBy { it.deadline }
        val scoreLookup = scores.associateBy { it.evidenceId }

        val learningCycles = groupEvidencesIntoCycles(chronologicalEvidences)
        val activeCycle = learningCycles.lastOrNull() ?: emptyList()
        val activeMonitoringEvidences = activeCycle.filter { it.type == EvidenceType.MONITORING }

        val missingSubmissionsCount = activeMonitoringEvidences.count { 
            it.deadline <= now && !scoreLookup.containsKey(it.evidenceId) 
        }

        val activeCycleGrades = activeMonitoringEvidences.mapNotNull { scoreLookup[it.evidenceId]?.score }
        val monitoringPerformance = if (activeCycleGrades.isNotEmpty()) activeCycleGrades.average() else null

        val studentAttendanceRecords = attendance.filter { it.studentId == student.studentId }
        val sessionsUntilNow = sessions.filter { it.timestamp <= now }
        val absencesCount = studentAttendanceRecords.count { record ->
            sessionsUntilNow.any { it.sessionId == record.sessionId } && record.status == AttendanceStatus.ABSENT
        }
        val absenceRate = if (totalPlannedSessions > 0) (absencesCount.toDouble() / totalPlannedSessions) * 100.0 else 0.0

        val performanceDiscrepancy = calculateDiscrepancy(monitoringPerformance, learningCycles, scoreLookup)

        val regularityState = when {
            missingSubmissionsCount >= 2 -> MonitoringState.CRITICAL
            missingSubmissionsCount == 1 -> MonitoringState.ATTENTION
            else -> MonitoringState.ON_TRACK
        }

        val performanceState = monitoringPerformance?.let {
            when {
                it < 4.0 -> MonitoringState.CRITICAL
                it < 6.0 -> MonitoringState.ATTENTION
                else -> MonitoringState.ON_TRACK
            }
        }

        val attendanceState = when {
            absenceRate >= ATTENDANCE_CRITICAL_THRESHOLD -> MonitoringState.CRITICAL
            absenceRate >= ATTENDANCE_ATTENTION_THRESHOLD -> MonitoringState.ATTENTION
            else -> MonitoringState.ON_TRACK
        }

        val discrepancyState = performanceDiscrepancy?.let {
            when {
                it >= 5.0 -> MonitoringState.CRITICAL
                it >= 3.0 -> MonitoringState.ATTENTION
                else -> MonitoringState.ON_TRACK
            }
        }

        val activeEvidenceType = chronologicalEvidences.lastOrNull { it.deadline <= now }?.type

        val operationalStatus = evaluateOperationalStatus(
            regularityState = regularityState,
            performanceState = performanceState,
            attendanceState = attendanceState,
            discrepancyState = discrepancyState,
            activeMonitoringEvidences = activeMonitoringEvidences,
            scoreLookup = scoreLookup,
            monitoringPerformance = monitoringPerformance
        )

        val recommendedActions = determineInterventionActions(
            regularityState = regularityState,
            performanceState = performanceState,
            attendanceState = attendanceState,
            discrepancyState = discrepancyState
        )

        return StudentMonitoringSummary(
            student = student,
            regularity = missingSubmissionsCount,
            regularityState = regularityState,
            performance = monitoringPerformance,
            performanceState = performanceState,
            attendance = absenceRate,
            attendanceState = attendanceState,
            discrepancy = performanceDiscrepancy,
            discrepancyState = discrepancyState,
            hasDiscrepancyFlag = performanceDiscrepancy?.let { it >= 3.0 } ?: false,
            actions = recommendedActions,
            activeEvidenceType = activeEvidenceType,
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
     * Compares the current cycle's Pm with the Consolidation Evidence (CE) grade of the same cycle.
     *
     * @param activeCyclePm The PM of the current active cycle.
     * @param cycles List of grouped learning cycles.
     * @param scoreLookup Map for student score retrieval.
     * @return The difference (Pm - CE) or 0.0 if data is insufficient.
     */
    private fun calculateDiscrepancy(
        activeCyclePm: Double?,
        cycles: List<List<Evidence>>,
        scoreLookup: Map<String, EvidenceScore>
    ): Double {
        val activeCycle = cycles.lastOrNull() ?: return 0.0
        val consolidationEvidence = activeCycle.find { it.type == EvidenceType.CONSOLIDATION }
        val consolidationGrade = consolidationEvidence?.let { scoreLookup[it.evidenceId]?.score }

        return if (activeCyclePm != null && consolidationGrade != null) {
            activeCyclePm - consolidationGrade
        } else {
            0.0
        }
    }

    /**
     * Determines the student's operational status based on trigger conditions.
     * Follows the pedagogical guidelines from monitoring.md, including discrepancy alerts.
     *
     * @param regularityState State of regularity.
     * @param performanceState State of performance.
     * @param attendanceState State of attendance.
     * @param discrepancyState State of discrepancy.
     * @param activeMonitoringEvidences Monitoring evidences of the active cycle.
     * @param scoreLookup Map for student score retrieval.
     * @param monitoringPerformance Current average Pm.
     * @return The derived [MonitoringState].
     */
    private fun evaluateOperationalStatus(
        regularityState: MonitoringState,
        performanceState: MonitoringState?,
        attendanceState: MonitoringState,
        discrepancyState: MonitoringState?,
        activeMonitoringEvidences: List<Evidence>,
        scoreLookup: Map<String, EvidenceScore>,
        monitoringPerformance: Double?
    ): MonitoringState {
        val individualGrades = activeMonitoringEvidences.mapNotNull { scoreLookup[it.evidenceId]?.score }
        
        // Critical Triggers:
        // - Two or more missing submissions (Regularity Critical)
        // - Pm < 4.0 (Performance Critical)
        // - Pm < 6.0 in two consecutive Monitoring Evidences (Specific rule)
        // - Attendance risk (Attendance Critical)
        // - Critical discrepancy (Discrepancy Critical)
        val hasTwoConsecutiveLowGrades = if (individualGrades.size >= 2) {
            individualGrades.windowed(2).any { window -> 
                window.all { it < MINIMUM_PASSING_GRADE } 
            }
        } else false
        
        if (regularityState == MonitoringState.CRITICAL || 
            performanceState == MonitoringState.CRITICAL || 
            hasTwoConsecutiveLowGrades || 
            attendanceState == MonitoringState.CRITICAL || 
            discrepancyState == MonitoringState.CRITICAL) {
            return MonitoringState.CRITICAL
        }

        // Attention Triggers:
        // - One missing submission (Regularity Attention)
        // - Pm < 6.0 caused by one Monitoring Evidence (Performance Attention)
        // - Attendance attention (Attendance Attention)
        // - Attention discrepancy (Discrepancy Attention)
        if (regularityState == MonitoringState.ATTENTION || 
            performanceState == MonitoringState.ATTENTION || 
            attendanceState == MonitoringState.ATTENTION || 
            discrepancyState == MonitoringState.ATTENTION) {
            return MonitoringState.ATTENTION
        }

        return MonitoringState.ON_TRACK
    }

    /**
     * Determines the recommended intervention actions based on indicator states.
     *
     * Follows the Intervention Matrix defined in the pedagogical guidelines.
     *
     * @param regularityState State of regularity.
     * @param performanceState State of performance.
     * @param attendanceState State of attendance.
     * @param discrepancyState State of discrepancy.
     * @return A deduplicated list of [InterventionAction] sorted by ID.
     */
    private fun determineInterventionActions(
        regularityState: MonitoringState,
        performanceState: MonitoringState?,
        attendanceState: MonitoringState,
        discrepancyState: MonitoringState?
    ): List<InterventionAction> {
        val actions = mutableSetOf<InterventionAction>()

        // Regularity
        when (regularityState) {
            MonitoringState.ATTENTION -> {
                actions.add(InterventionAction.A1)
                actions.add(InterventionAction.A2)
                actions.add(InterventionAction.A4)
            }
            MonitoringState.CRITICAL -> {
                actions.add(InterventionAction.A1)
                actions.add(InterventionAction.A2)
                actions.add(InterventionAction.A4)
                actions.add(InterventionAction.A6)
            }
            else -> {}
        }

        // Performance
        when (performanceState) {
            MonitoringState.ATTENTION -> {
                actions.add(InterventionAction.A3)
                actions.add(InterventionAction.A5)
            }
            MonitoringState.CRITICAL -> {
                actions.add(InterventionAction.A3)
                actions.add(InterventionAction.A5)
                actions.add(InterventionAction.A6)
            }
            else -> {}
        }

        // Attendance
        when (attendanceState) {
            MonitoringState.ATTENTION -> {
                actions.add(InterventionAction.A1)
                actions.add(InterventionAction.A2)
            }
            MonitoringState.CRITICAL -> {
                actions.add(InterventionAction.A1)
                actions.add(InterventionAction.A2)
                actions.add(InterventionAction.A6)
                actions.add(InterventionAction.A7)
            }
            else -> {}
        }

        // Performance Discrepancy
        when (discrepancyState) {
            MonitoringState.ATTENTION -> {
                actions.add(InterventionAction.A5)
            }
            MonitoringState.CRITICAL -> {
                actions.add(InterventionAction.A1)
                actions.add(InterventionAction.A2)
                actions.add(InterventionAction.A5)
                actions.add(InterventionAction.A6)
            }
            else -> {}
        }

        return actions.sortedBy { it.id }
    }
}
