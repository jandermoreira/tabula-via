package edu.jm.tabulavia.logic

import edu.jm.tabulavia.model.*
import edu.jm.tabulavia.utils.toSkillLevel

/**
 * The "Brain" of the monitoring system.
 * Processes raw evidence scores to derive diagnostic indicators.
 */
object MonitoringCalculator {

    /**
     * Calculates the monitoring summary for a student based on their historical performance.
     */
    fun calculate(
        studentId: String,
        classId: String,
        evidences: List<Evidence>,
        scores: List<EvidenceScore>
    ): StudentMonitoringSummary {
        // 1. Chronological sorting is essential for trend and consistency analysis
        val sortedEvidences = evidences.sortedBy { it.deadline }
        val scoreMap = scores.associateBy { it.evidenceId }

        // 2. Derive basic indicators
        val currentLevel = calculateCurrentLevel(sortedEvidences, scoreMap)
        val trend = calculateTrend(sortedEvidences, scoreMap)
        val isConsistent = calculateConsistency(sortedEvidences, scoreMap)

        // 3. Determine the diagnostic state
        val state = determineState(sortedEvidences, scoreMap)

        return StudentMonitoringSummary(
            studentId = studentId,
            classId = classId,
            currentLevel = currentLevel,
            trend = trend,
            isConsistent = isConsistent,
            needsIntervention = state != MonitoringState.NORMAL,
            state = state
        )
    }

    private fun calculateCurrentLevel(
        evidences: List<Evidence>,
        scoreMap: Map<String, EvidenceScore>
    ): SkillLevel {
        val lastEvidenceWithScore = evidences.lastOrNull { scoreMap.containsKey(it.evidenceId) }
        return lastEvidenceWithScore?.let {
            scoreMap[it.evidenceId]?.score?.toSkillLevel()
        } ?: SkillLevel.NOT_APPLICABLE
    }

    private fun calculateTrend(
        evidences: List<Evidence>,
        scoreMap: Map<String, EvidenceScore>
    ): EvidenceTrend {
        val validScores = evidences.mapNotNull { scoreMap[it.evidenceId]?.score }
        if (validScores.size < 2) return EvidenceTrend.UNKNOWN

        val current = validScores.last()
        val previous = validScores[validScores.size - 2]

        return when {
            current > previous + 0.05 -> EvidenceTrend.IMPROVED
            current < previous - 0.05 -> EvidenceTrend.WORSENED
            else -> EvidenceTrend.STABLE
        }
    }

    private fun calculateConsistency(
        evidences: List<Evidence>,
        scoreMap: Map<String, EvidenceScore>
    ): Boolean {
        // Analysis of the last 3 applicable evidences
        val recentLevels = evidences.takeLast(3)
            .mapNotNull { scoreMap[it.evidenceId]?.score?.toSkillLevel() }

        if (recentLevels.size < 2) return true
        return recentLevels.distinct().size == 1
    }

    private fun determineState(
        evidences: List<Evidence>,
        scoreMap: Map<String, EvidenceScore>
    ): MonitoringState {
        if (evidences.isEmpty()) return MonitoringState.NORMAL

        // Check RECOVERY: Low performance in a Consolidation evidence (like a final exam)
        val lastConsolidation = evidences.lastOrNull { it.type == EvidenceType.CONSOLIDATION }
        val consolidationScore = lastConsolidation?.let { scoreMap[it.evidenceId]?.score }
        if (consolidationScore != null && consolidationScore.toSkillLevel() == SkillLevel.LOW) {
            return MonitoringState.RECOVERY
        }

        // Check PRIORITY: Persistent difficulties or missing data (rhythm failure)
        val recentEvidences = evidences.takeLast(3)
        val lowCount = recentEvidences.count { 
            scoreMap[it.evidenceId]?.score?.toSkillLevel() == SkillLevel.LOW 
        }
        val missingCount = recentEvidences.count { !scoreMap.containsKey(it.evidenceId) }

        if (lowCount >= 2 || missingCount >= 2) {
            return MonitoringState.PRIORITY
        }

        // Check REVIEW: Localized difficulty in a single monitoring evidence
        val lastMonitoring = evidences.lastOrNull { it.type == EvidenceType.MONITORING }
        val monitoringScore = lastMonitoring?.let { scoreMap[it.evidenceId]?.score }
        if (monitoringScore != null && monitoringScore.toSkillLevel() == SkillLevel.LOW) {
            return MonitoringState.REVIEW
        }

        return MonitoringState.NORMAL
    }
}
