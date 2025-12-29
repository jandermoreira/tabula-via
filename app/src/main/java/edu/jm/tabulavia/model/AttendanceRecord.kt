package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable // Anotação para serialização
@Entity(
    tableName = "attendance_records",
    primaryKeys = ["sessionId", "studentId"],
    foreignKeys = [
        ForeignKey(entity = ClassSession::class, parentColumns = ["sessionId"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["studentId"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
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

object SkillConsolidator {

    // Base weights from documentation (Item 8)
    private const val OBSERVATION_BASE_WEIGHT = 0.50
    private const val SELF_ASSESSMENT_BASE_WEIGHT = 0.25
    private const val PEER_BASE_WEIGHT = 0.25

    /**
     * Consolidates different assessment sources into a single value.
     * Applies reliability adjustment (f(n)) based on peer evaluation volume.
     */
    fun consolidate(
        observationScore: Double,
        selfAssessmentScore: Double,
        peerScores: List<Double>,
        targetPeerCount: Int = 5 // N parameter from documentation
    ): Double {

        val peerCount = peerScores.size

        // 1. Peer Consolidation (Item 7)
        val peerConsolidatedScore = when {
            peerCount == 0 -> 0.0
            peerCount <= 3 -> calculateMedian(peerScores)
            else -> peerScores.average() // Mean for n >= 5 (and n=4 as policy)
        }

        // 2. Weight Adjustment by peer volume (Item 9: f(n) = min(1, n/N))
        val reliabilityFactor = min(1.0, peerCount.toDouble() / targetPeerCount)
        val adjustedPeerWeight = PEER_BASE_WEIGHT * reliabilityFactor

        // 3. Renormalization (Item 9: weights must sum to 1.0)
        val totalWeightRaw = OBSERVATION_BASE_WEIGHT + SELF_ASSESSMENT_BASE_WEIGHT + adjustedPeerWeight

        val finalObservationWeight = OBSERVATION_BASE_WEIGHT / totalWeightRaw
        val finalSelfWeight = SELF_ASSESSMENT_BASE_WEIGHT / totalWeightRaw
        val finalPeerWeight = adjustedPeerWeight / totalWeightRaw

        // 4. Final Calculation (Item 10)
        return (observationScore * finalObservationWeight) +
                (selfAssessmentScore * finalSelfWeight) +
                (peerConsolidatedScore * finalPeerWeight)
    }

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