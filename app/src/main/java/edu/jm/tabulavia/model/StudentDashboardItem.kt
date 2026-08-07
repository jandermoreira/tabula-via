package edu.jm.tabulavia.model

/**
 * UI model representing a student's diagnostic status in the class dashboard.
 */
data class StudentDashboardItem(
    val student: Student,
    val summary: StudentMonitoringSummary,
    /**
     * Historical list of evidences and scores for the student.
     */
    val evidenceHistory: List<EvidenceHistoryItem> = emptyList()
)

/**
 * Represents a single evidence entry in the student's history,
 * including a snapshot of indicators at that point in time.
 */
data class EvidenceHistoryItem(
    val evidenceName: String,
    val deadline: Long,
    val score: Double?,
    val snapshot: StudentMonitoringSummary
)
