package edu.jm.tabulavia.model

/**
 * Holds the calculated monitoring indicators for a student in a specific class.
 * These values are derived from the chronological analysis of EvidenceScore records.
 */
data class StudentMonitoringSummary(
    val studentId: String,
    val classId: String,
    val currentLevel: SkillLevel,
    val trend: EvidenceTrend,
    val isConsistent: Boolean,
    val needsIntervention: Boolean,
    val state: MonitoringState
)
