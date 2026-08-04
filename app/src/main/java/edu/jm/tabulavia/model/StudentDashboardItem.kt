package edu.jm.tabulavia.model

/**
 * UI model representing a student's diagnostic status in the class dashboard.
 */
data class StudentDashboardItem(
    val student: Student,
    val state: StudentTrackingState,
    val trend: EvidenceTrend,
    val isConsistent: Boolean,
    val currentLevel: SkillState,
    val lastScore: Double? = null,
    /**
     * Historical list of evidences and scores for the student.
     */
    val evidenceHistory: List<EvidenceHistoryItem> = emptyList()
)

/**
 * Represents a single evidence entry in the student's history.
 */
data class EvidenceHistoryItem(
    val evidenceName: String,
    val deadline: Long,
    val score: Double
)
