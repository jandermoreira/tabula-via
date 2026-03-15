/**
 * AttendanceRecord entity for the 'attendance_records' table.
 * Links students to class sessions using String identifiers for persistent synchronization.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlin.math.min

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

    /**
     * Unique identifier of the session, referencing ClassSession.sessionId.
     */
    val sessionId: String = "",

    /**
     * Unique identifier of the student, referencing Student.studentId.
     */
    val studentId: String = "",

    /**
     * Status of the student's attendance in the session.
     * Defaults to ABSENT to prevent null pointer exceptions during deserialization.
     */
    val status: AttendanceStatus = AttendanceStatus.ABSENT
)

/**
 * Utility object for calculating final skill scores by merging different assessment sources.
 * Applies specific weights to observations, self-assessments, and peer reviews.
 */
object SkillConsolidator {

    private const val OBSERVATION_BASE_WEIGHT = 0.50
    private const val SELF_ASSESSMENT_BASE_WEIGHT = 0.25
    private const val PEER_BASE_WEIGHT = 0.25

    /**
     * Merges observation, self-assessment, and peer scores into a single consolidated value.
     * Adjusts the peer weight based on the number of evaluations received to ensure reliability.
     */
    fun consolidate(
        observationScore: Double,
        selfAssessmentScore: Double,
        peerScores: List<Double>,
        targetPeerCount: Int = 5
    ): Double {

        val peerCount = peerScores.size

        // Selects the calculation method based on the amount of peer data available
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
     * Used for peer scores when the sample size is small (3 or fewer).
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