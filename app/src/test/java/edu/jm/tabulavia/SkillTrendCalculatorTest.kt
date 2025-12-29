package edu.jm.tabulavia

import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.SkillStatus
import edu.jm.tabulavia.model.SkillTrend
import edu.jm.tabulavia.utils.SkillTrendCalculator
import edu.jm.tabulavia.utils.TrendCalculationMethod
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class SkillTrendCalculatorTest {

    @Test
    fun linearRegressionShouldReturnImprovingWhenScoresIncrease() {
        // Timeline: 1 -> 2 -> 3
        val history = listOf(
            createMockStatus(level = SkillLevel.LOW, timestamp = 1000L),
            createMockStatus(level = SkillLevel.MEDIUM, timestamp = 2000L),
            createMockStatus(level = SkillLevel.HIGH, timestamp = 3000L)
        )

        val result = SkillTrendCalculator.calculateTrend(
            assessments = history,
            method = TrendCalculationMethod.LINEAR_REGRESSION
        )

        Assert.assertEquals(SkillTrend.IMPROVING, result)
    }

    @Test
    fun linearRegressionShouldReturnDecliningWhenScoresDecrease() {
        // Timeline: 3 -> 2 -> 1
        val history = listOf(
            createMockStatus(level = SkillLevel.HIGH, timestamp = 1000L),
            createMockStatus(level = SkillLevel.MEDIUM, timestamp = 2000L),
            createMockStatus(level = SkillLevel.LOW, timestamp = 3000L)
        )

        val result = SkillTrendCalculator.calculateTrend(
            assessments = history,
            method = TrendCalculationMethod.LINEAR_REGRESSION
        )

        Assert.assertEquals(SkillTrend.DECLINING, result)
    }

    @Test
    fun linearRegressionShouldReturnStableWhenScoresAreIdentical() {
        val history = listOf(
            createMockStatus(level = SkillLevel.MEDIUM, timestamp = 1000L),
            createMockStatus(level = SkillLevel.MEDIUM, timestamp = 2000L)
        )

        val result = SkillTrendCalculator.calculateTrend(
            assessments = history,
            method = TrendCalculationMethod.LINEAR_REGRESSION
        )

        Assert.assertEquals(SkillTrend.STABLE, result)
    }

    @Test
    fun trendShouldReturnStableWhenScoresAreNotApplicable() {
        val history = listOf(
            createMockStatus(level = SkillLevel.NOT_APPLICABLE, timestamp = 1000L),
            createMockStatus(level = SkillLevel.NOT_APPLICABLE, timestamp = 2000L)
        )

        val result = SkillTrendCalculator.calculateTrend(
            assessments = history,
            method = TrendCalculationMethod.SIMPLE_DIFFERENCE
        )

        Assert.assertEquals(SkillTrend.STABLE, result)
    }

    @Test
    fun trendShouldReturnStableWhenHistoryIsEmpty() {
        val emptyHistory = emptyList<SkillStatus>()

        val result = SkillTrendCalculator.calculateTrend(
            assessments = emptyHistory,
            method = TrendCalculationMethod.LINEAR_REGRESSION
        )

        assertEquals(SkillTrend.STABLE, result)
    }

    @Test
    fun movingAverageShouldReturnImprovingWhenAverageRises() {
        // History: 1, 1, 3 -> Average of last 2 is 2.0 (higher than 1.0)
        val history = listOf(
            createMockStatus(level = SkillLevel.LOW, timestamp = 1000L),
            createMockStatus(level = SkillLevel.LOW, timestamp = 2000L),
            createMockStatus(level = SkillLevel.HIGH, timestamp = 3000L)
        )

        val result = SkillTrendCalculator.calculateTrend(
            assessments = history,
            method = TrendCalculationMethod.MOVING_AVERAGE,
            historyCount = 2 // Let's use a window of 2
        )

        assertEquals(SkillTrend.IMPROVING, result)
    }

    /**
     * Helper to create SkillStatus.
     * Note: Ensure SkillStatus constructor matches these arguments.
     */
    private fun createMockStatus(
        level: SkillLevel,
        timestamp: Long,
        count: Int = 1,
        trend: SkillTrend = SkillTrend.STABLE
    ): SkillStatus {
        return SkillStatus(
            skillName = "Communication",
            currentLevel = level,
            lastAssessedTimestamp = timestamp,
            assessmentCount = count,
            trend = trend
        )
    }
}