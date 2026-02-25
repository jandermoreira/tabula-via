/**
 * Data model for student attendance records and logic for skill score consolidation.
 */

package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlin.math.min

/**
 * Represents the attendance status of a student in a specific class session.
 * Linked to ClassSession and Student entities via foreign keys.
 */
@Serializable
@Entity(
    tableName = "attendance_records",
    primaryKeys = ["sessionId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = ClassSession::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("studentId")
    ]
)
data class AttendanceRecord(
    val sessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus
)

/**
 * Utility object for calculating final skill scores by merging different assessment sources.
 */
object SkillConsolidator {

    private const val OBSERVATION_BASE_WEIGHT = 0.50
    private const val SELF_ASSESSMENT_BASE_WEIGHT = 0.25
    private const val PEER_BASE_WEIGHT = 0.25

    /**
     * Merges observation, self-assessment, and peer scores into a single consolidated value.
     * Adjusts the peer weight based on the number of evaluations received.
     */
    fun consolidate(
        observationScore: Double,
        selfAssessmentScore: Double,
        peerScores: List<Double>,
        targetPeerCount: Int = 5
    ): Double {

        val peerCount = peerScores.size

        // Determine the representative peer score based on sample size
        val peerConsolidatedScore = when {
            peerCount == 0 -> 0.0
            peerCount <= 3 -> calculateMedian(peerScores)
            else -> peerScores.average()
        }

        // Apply reliability factor f(n) = min(1, n/N) to the peer weight
        val reliabilityFactor = min(1.0, peerCount.toDouble() / targetPeerCount)
        val adjustedPeerWeight = PEER_BASE_WEIGHT * reliabilityFactor

        // Renormalize all weights so their sum equals 1.0
        val totalWeightRaw =
            OBSERVATION_BASE_WEIGHT + SELF_ASSESSMENT_BASE_WEIGHT + adjustedPeerWeight

        val finalObservationWeight = OBSERVATION_BASE_WEIGHT / totalWeightRaw
        val finalSelfWeight = SELF_ASSESSMENT_BASE_WEIGHT / totalWeightRaw
        val finalPeerWeight = adjustedPeerWeight / totalWeightRaw

        // Calculate final weighted average
        return (observationScore * finalObservationWeight) +
                (selfAssessmentScore * finalSelfWeight) +
                (peerConsolidatedScore * finalPeerWeight)
    }

    /**
     * Calculates the median value of a list of doubles.
     */
    private fun calculateMedian(list: List<Double>): Double {
        if (list.isEmpty()) return 0.0
        val sorted = list.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
    }
}