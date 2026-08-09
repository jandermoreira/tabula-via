/**
 * Data structures for student monitoring summaries.
 */
package edu.jm.tabulavia.model

/**
 * Summary of a student's monitoring status and recommendations.
 *
 * Holds the calculated monitoring indicators for a student in a specific class
 * according to the Student Work Rhythm Monitoring guidelines.
 *
 * @property student The student being monitored.
 * @property regularity The number of missing monitoring submissions.
 * @property regularityState The state classification for regularity.
 * @property performance The average performance in monitoring activities, if any.
 * @property performanceState The state classification for performance, if applicable.
 * @property attendance The attendance percentage.
 * @property attendanceState The state classification for attendance.
 * @property discrepancy The calculated performance discrepancy, if applicable.
 * @property discrepancyState The state classification for discrepancy, if applicable.
 * @property hasDiscrepancyFlag Indicates if the discrepancy exceeds the attention threshold.
 * @property actions List of recommended intervention actions based on the current state.
 * @property activeEvidenceType The type of the last processed evidence that determined the current state.
 * @property state The overall operational monitoring state of the student.
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
    val actions: List<InterventionAction> = emptyList(),
    val activeEvidenceType: EvidenceType? = null,
    val state: MonitoringState
)
