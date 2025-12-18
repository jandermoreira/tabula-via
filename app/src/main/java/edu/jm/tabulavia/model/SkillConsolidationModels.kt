package edu.jm.tabulavia.model


/**
 * TECHNICAL DOCUMENTATION
 *
 * Skill Consolidation and Evolution Module
 *
 * -------------------------------------------------------------------------
 *
 * 1. Objective
 *
 * This module defines the process for recording, consolidating, and tracking
 * skill trends for individual students, based on three asymmetric assessment
 * sources:
 *
 * - Teacher observation
 * - Student self-assessment
 * - Peer assessment
 *
 * The objective is to produce, for each student, skill, and point in time,
 * a consolidated value that is stable, comparable over time, and robust
 * against volume bias.
 *
 * -------------------------------------------------------------------------
 *
 * 2. Scope
 *
 * This document specifies:
 *
 * - Valid system inputs
 * - Temporal selection rules for records
 * - Aggregation methods
 * - Consolidated value calculation
 * - Historical records for trend analysis
 *
 * It does not define user interfaces or pedagogical policies.
 *
 * -------------------------------------------------------------------------
 *
 * 3. Definitions
 *
 * 3.1 Skill
 *
 * An observable competency evaluated over time
 * (e.g., Participation, Communication, Listening, etc.).
 *
 * 3.2 Assessment record
 *
 * An individual assessment entry associated with:
 *
 * - student
 * - skill
 * - source type
 * - value
 * - timestamp
 *
 * 3.3 Source types
 *
 * - Observation: single teacher assessment
 * - Self-assessment: single student assessment
 * - Peer: multiple assessments provided by classmates
 *
 * -------------------------------------------------------------------------
 *
 * 4. Value scale
 *
 * All assessments operate on the same numeric scale:
 *
 * - 1 = low level
 * - 2 = medium level
 * - 3 = high level
 *
 * Fractional values exist only after consolidation,
 * never as raw input.
 *
 * -------------------------------------------------------------------------
 *
 * 5. Data model (conceptual)
 *
 * For each assessment record:
 *
 * - studentId
 * - skillId
 * - sourceType ∈ {OBSERVATION, SELF_ASSESSMENT, PEER}
 * - value ∈ {1, 2, 3}
 * - timestamp
 *
 * For each calculated consolidation:
 *
 * - studentId
 * - skillId
 * - referenceDate
 * - observationValue
 * - selfAssessmentValue
 * - peerConsolidatedValue
 * - peerEvaluationCount
 * - effectiveWeights
 * - consolidatedValue
 *
 * -------------------------------------------------------------------------
 *
 * 6. Data selection rules (volume bias)
 *
 * Due to asymmetry between sources, different rules apply:
 *
 * 6.1 Teacher observation
 *
 * - Always use the most recent record applicable to the skill.
 * - Only one value enters the calculation.
 *
 * 6.2 Student self-assessment
 *
 * - Always use the most recent record applicable to the skill.
 * - Only one value enters the calculation.
 *
 * 6.3 Peer assessment
 *
 * - Select the N most recent assessments applicable to the skill.
 * - N is a system parameter (e.g., N = 5).
 * - These assessments are aggregated into a single
 *   peerConsolidatedValue.
 *
 * This design prevents a large volume of peer assessments
 * from dominating consolidation through recency.
 *
 * -------------------------------------------------------------------------
 *
 * 7. Peer assessment consolidation
 *
 * Given the set P = {P₁, …, Pₙ}:
 *
 * - If n = 0 → no peer value exists
 * - If n ≤ 3 → peerConsolidatedValue = MEDIAN(P)
 * - If n ≥ 5 → peerConsolidatedValue = MEAN(P)
 * - If n = 4 → fixed system policy (MEDIAN or MEAN, but consistent)
 *
 * In all cases, peerEvaluationCount must be recorded.
 *
 * -------------------------------------------------------------------------
 *
 * 8. Base weights of sources
 *
 * The system base weights are defined as:
 *
 * - Observation: 0.50
 * - Self-assessment: 0.25
 * - Peer: 0.25
 *
 * These weights represent pedagogical intent,
 * prior to reliability adjustments.
 *
 * -------------------------------------------------------------------------
 *
 * 9. Weight adjustment by peer volume
 *
 * When peerEvaluationCount is small, the reliability of the
 * peer consolidated value is lower.
 *
 * Adjustment function:
 *
 * f(n) = min(1, n / N)
 *
 * Effective peer weight:
 *
 * peerWeight = peerBaseWeight × f(n)
 *
 * Final weights are renormalized:
 *
 * observationWeight    = W_O / (W_O + W_A + W_P)
 * selfAssessmentWeight = W_A / (W_O + W_A + W_P)
 * peerWeight           = W_P / (W_O + W_A + W_P)
 *
 * If no peer assessments exist (n = 0), the peer weight is removed
 * and observation/self-assessment weights are renormalized.
 *
 * -------------------------------------------------------------------------
 *
 * 10. Consolidated value calculation
 *
 * With all values defined:
 *
 * consolidatedValue =
 *     observationWeight    × observationValue +
 *     selfAssessmentWeight × selfAssessmentValue +
 *     peerWeight           × peerConsolidatedValue
 *
 * This is the only value stored for longitudinal analysis.
 *
 * -------------------------------------------------------------------------
 *
 * 11. Historical record
 *
 * For each consolidation, the system must store:
 *
 * - consolidatedValue
 * - referenceDate
 * - peerEvaluationCount
 * - peerAggregationMethod
 * - effectiveWeights
 *
 * This ensures traceability and auditability.
 *
 * -------------------------------------------------------------------------
 *
 * 12. Mapping to interpretive levels
 *
 * The consolidated value (1.00 to 3.00) is mapped to levels:
 *
 * - 1.00 – 1.66 → Low
 * - 1.67 – 2.32 → Medium
 * - 2.33 – 3.00 → High
 *
 * -------------------------------------------------------------------------
 *
 * 13. Trend calculation
 *
 * Skill trends are calculated exclusively over the
 * consolidated value time series.
 *
 * Allowed methods:
 *
 * - simple difference between consecutive periods
 * - moving average
 * - linear regression (when sufficient data points exist)
 *
 * Raw assessment inputs are never used in trend analysis.
 *
 * -------------------------------------------------------------------------
 *
 * 14. Model guarantees
 *
 * - Robustness against volume bias
 * - Temporal comparability
 * - Statistical stability
 * - Calculation transparency
 * - Deterministic implementation
 *
 */
