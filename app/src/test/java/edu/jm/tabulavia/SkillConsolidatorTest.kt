package edu.jm.tabulavia.utils

import edu.jm.tabulavia.model.SkillConsolidator
import edu.jm.tabulavia.model.SkillLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SkillConsolidatorTest {

    @Test
    fun shouldCalculateConsolidatedValueWithFullPeerReliability() {
        // Scenario: Everyone agrees on 3.0. Peer count (5) meets targetPeerCount (5).
        // Weights should be: Observation 0.50, Self 0.25, Peer 0.25
        val consolidatedValue = SkillConsolidator.consolidate(
            observationScore = 3.0,
            selfAssessmentScore = 3.0,
            peerScores = listOf(3.0, 3.0, 3.0, 3.0, 3.0),
            targetPeerCount = 5
        )

        // Result: (3.0 * 0.5) + (3.0 * 0.25) + (3.0 * 0.25) = 3.0
        assertEquals(3.0, consolidatedValue, 0.01)
    }

    @Test
    fun shouldRenormalizeWeightsWhenPeerDataIsMissing() {
        // Scenario: Only Teacher (score 3) and Student (score 1) provided data.
        // Base weights: Observation 0.50, Self 0.25, Peer 0.00 (because list is empty)
        // Normalization:
        // Total = 0.50 + 0.25 = 0.75
        // Final Observation Weight = 0.50 / 0.75 = 0.666...
        // Final Self Weight = 0.25 / 0.75 = 0.333...

        val consolidatedValue = SkillConsolidator.consolidate(
            observationScore = 3.0,
            selfAssessmentScore = 1.0,
            peerScores = emptyList(),
            targetPeerCount = 5
        )

        // Calculation: (3.0 * 0.6667) + (1.0 * 0.3333) = 2.0 + 0.3333 = 2.3333
        assertEquals(2.333, consolidatedValue, 0.001)
    }

    @Test
    fun shouldApplyMedianWhenPeerCountIsSmall() {
        // Scenario: 3 peers with an outlier (1.0, 1.0, 3.0).
        // Item 7 of doc says: n <= 3 -> use MEDIAN. Median is 1.0.
        // Reliability factor f(n): 3/5 = 0.6
        // Adjusted Peer Weight: 0.25 * 0.6 = 0.15

        val consolidatedValue = SkillConsolidator.consolidate(
            observationScore = 2.0,
            selfAssessmentScore = 2.0,
            peerScores = listOf(1.0, 1.0, 3.0),
            targetPeerCount = 5
        )

        // If it used Mean (1.66), the result would be different.
        // With Median (1.0), we prove the protection against outliers works.
        // Total weight = 0.50 + 0.25 + 0.15 = 0.90
        // Result: ((2.0 * 0.5) + (2.0 * 0.25) + (1.0 * 0.15)) / 0.90
        // (1.0 + 0.5 + 0.15) / 0.9 = 1.65 / 0.9 = 1.8333
        assertEquals(1.833, consolidatedValue, 0.001)
    }

    @Test
    fun shouldMapScoreToCorrectInterpretiveLevel() {
        assertEquals(SkillLevel.LOW, 1.5.toSkillLevel())
        assertEquals(SkillLevel.MEDIUM, 2.0.toSkillLevel())
        assertEquals(SkillLevel.HIGH, 2.5.toSkillLevel())
    }
}