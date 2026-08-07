package edu.jm.tabulavia.model

/**
 * Holds the calculated monitoring indicators for a student in a specific class
 * according to the Student Work Rhythm Monitoring guidelines.
 */
data class StudentMonitoringSummary(
    val student: Student,
    val regularity: Int,
    val regularityState: MonitoringState,
    val performance: Double?,
    val performanceState: MonitoringState?,
    val attendance: Double,
    val attendanceState: MonitoringState,
    val discrepancy: Double?,
    val discrepancyState: MonitoringState?,
    val hasDiscrepancyFlag: Boolean,
    val state: MonitoringState
)
