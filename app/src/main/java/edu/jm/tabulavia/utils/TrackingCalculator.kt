package edu.jm.tabulavia.utils

import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceHistoryItem
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceTrend
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.SkillState
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentDashboardItem
import edu.jm.tabulavia.model.StudentTrackingState

/**
 * Utility to calculate pedagogical tracking indicators for students.
 * Implementation based on the "Intelligent Individual Tracking Plan" specification.
 */
object TrackingCalculator {

    private const val SCORE_LOW_THRESHOLD = 5.0
    private const val SCORE_HIGH_THRESHOLD = 8.0

    /**
     * Calculates the dashboard item for a student based on their scores and evidence metadata.
     *
     * @param student The student to evaluate.
     * @param evidences All evidences for the class, ordered by deadline.
     * @param scores All scores for this specific student.
     */
    fun calculateDashboardItem(
        student: Student,
        evidences: List<Evidence>,
        scores: List<EvidenceScore>
    ): StudentDashboardItem {
        val sortedScores = scores.associateBy { it.evidenceId }
        val chronologicallySortedEvidences = evidences.sortedBy { it.deadline }
        
        val studentScores = chronologicallySortedEvidences.mapNotNull { evidence ->
            sortedScores[evidence.evidenceId]?.let { it to evidence }
        }

        val history = studentScores.map { (score, evidence) ->
            EvidenceHistoryItem(
                evidenceName = evidence.name,
                deadline = evidence.deadline,
                score = score.score
            )
        }

        if (studentScores.isEmpty()) {
            return StudentDashboardItem(
                student = student,
                state = StudentTrackingState.NORMAL,
                trend = EvidenceTrend.UNKNOWN,
                isConsistent = true,
                currentLevel = SkillState.NOT_APPLICABLE,
                evidenceHistory = emptyList()
            )
        }

        val lastScorePair = studentScores.last()
        val lastScore = lastScorePair.first.score
        
        val currentLevel = mapScoreToLevel(lastScore)
        val trend = calculateTrend(studentScores)
        val isConsistent = calculateConsistency(studentScores)
        val state = determineState(studentScores, currentLevel)

        return StudentDashboardItem(
            student = student,
            state = state,
            trend = trend,
            isConsistent = isConsistent,
            currentLevel = currentLevel,
            lastScore = lastScore,
            evidenceHistory = history
        )
    }

    private fun mapScoreToLevel(score: Double): SkillState = when {
        score < SCORE_LOW_THRESHOLD -> SkillState.LOW
        score >= SCORE_HIGH_THRESHOLD -> SkillState.HIGH
        else -> SkillState.MEDIUM
    }

    private fun calculateTrend(history: List<Pair<EvidenceScore, Evidence>>): EvidenceTrend {
        if (history.size < 2) return EvidenceTrend.UNKNOWN
        val current = history.last().first.score
        val previous = history[history.size - 2].first.score
        
        return when {
            current > previous -> EvidenceTrend.IMPROVED
            current < previous -> EvidenceTrend.WORSENED
            else -> EvidenceTrend.STABLE
        }
    }

    private fun calculateConsistency(history: List<Pair<EvidenceScore, Evidence>>): Boolean {
        if (history.size < 2) return true
        val recentScores = history.takeLast(3).map { it.first.score }
        if (recentScores.size < 2) return true
        
        val average = recentScores.average()
        val variance = recentScores.map { (it - average) * (it - average) }.average()
        // Threshold for consistency (low variance)
        return variance < 2.0 
    }

    private fun determineState(
        history: List<Pair<EvidenceScore, Evidence>>,
        currentLevel: SkillState
    ): StudentTrackingState {
        val (lastScore, lastEvidence) = history.last()

        // 1. RECOVERY: Low performance confirmed by consolidation
        if (lastEvidence.type == EvidenceType.CONSOLIDATION && currentLevel == SkillState.LOW) {
            return StudentTrackingState.RECOVERY
        }

        // 2. PRIORITIZED: Persistent difficulties in monitoring
        val monitoringHistory = history.filter { it.second.type == EvidenceType.MONITORING }
        if (monitoringHistory.size >= 2) {
            val lastTwoLevels = monitoringHistory.takeLast(2).map { mapScoreToLevel(it.first.score) }
            if (lastTwoLevels.all { it == SkillState.LOW }) {
                return StudentTrackingState.PRIORITIZED_TRACKING
            }
        }

        // 3. GUIDED REVISION: Single localized difficulty in monitoring
        if (lastEvidence.type == EvidenceType.MONITORING && currentLevel == SkillState.LOW) {
            return StudentTrackingState.GUIDED_REVISION
        }

        return StudentTrackingState.NORMAL
    }
}
