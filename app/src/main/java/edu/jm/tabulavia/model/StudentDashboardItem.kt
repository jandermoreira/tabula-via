package edu.jm.tabulavia.model

/**
 * UI model representing a student's diagnostic status in the class dashboard.
 *
 * Consolidates student identification with calculated pedagogical indicators
 * derived from evidence analysis.
 */
data class StudentDashboardItem(
    /**
     * The student entity containing identification and status.
     */
    val student: Student,

    /**
     * The calculated tracking state (Normal, Revision, Prioritized, Recovery).
     */
    val state: StudentTrackingState,

    /**
     * The performance trend compared to the previous evidence.
     */
    val trend: EvidenceTrend,

    /**
     * Indicates if the student's performance is stable across recent evidences.
     */
    val isConsistent: Boolean,

    /**
     * The current proficiency level (Low, Medium, High).
     */
    val currentLevel: SkillState,

    /**
     * The score obtained in the most recent evidence.
     */
    val lastScore: Double? = null
)
