/**
 * SkillLevel.kt
 *
 * Defines the proficiency levels for student skills, including
 * their display names and associated numeric scores for calculation.
 */

package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Represents the proficiency level of a skill.
 *
 * @property displayName The Portuguese name of the level for UI display.
 * @property score The numeric value used for consolidating multiple assessments.
 */
@Serializable
enum class SkillLevel(
    val displayName: String,
    val score: Int?
) {
    /** The skill was not assessed or is not relevant in the current context. */
    NOT_APPLICABLE("Não se Aplica", null),

    /** Basic or introductory proficiency level. */
    LOW("Baixo", 1),

    /** Intermediate or developing proficiency level. */
    MEDIUM("Médio", 2),

    /** Advanced or proficient level. */
    HIGH("Alto", 3);

    /**
     * Checks if the skill level has an associated numeric score.
     *
     * @return True if the level is applicable for calculations, false otherwise.
     */
    fun isApplicable(): Boolean = score != null
}
