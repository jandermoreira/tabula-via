/**
 * SkillAssessmentsSummary.kt
 *
 * Provides a consolidated summary of all assessments for a specific skill,
 * including evaluations from the professor, the student, and peers.
 */

package edu.jm.tabulavia.model

/**
 * Data class representing the summary of assessments for a single skill.
 *
 * @property skillName The name of the skill being summarized.
 * @property professorAssessment The assessment provided by the professor, if any.
 * @property selfAssessment The self-assessment provided by the student, if any.
 * @property peerAssessment The assessment provided by peers, if any.
 */
data class SkillAssessmentsSummary(
    val skillName: String,
    val professorAssessment: SkillAssessment? = null,
    val selfAssessment: SkillAssessment? = null,
    val peerAssessment: SkillAssessment? = null
)
