package edu.jm.tabulavia.model

/**
 * Holds the calculated monitoring indicators for a student in a specific class
 * according to the Student Work Rhythm Monitoring guidelines.
 */
data class StudentMonitoringSummary(
    val student: Student,
    val regularity: Int,
    val performance: Double?,
    val attendance: Double,
    val discrepancy: Double?,
    val hasDiscrepancyFlag: Boolean,
    val state: MonitoringState
)
