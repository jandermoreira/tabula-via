/**
 * SkillState.kt
 *
 * Defines the possible states of proficiency for a skill.
 * Used for display and filtering in reports and dashboards.
 */

package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Represents the final state of a skill after consolidation.
 *
 * @property displayName The Portuguese name of the state for UI display.
 */
@Serializable
enum class SkillState(val displayName: String) {
    /** High proficiency level. */
    HIGH("Alto"),

    /** Medium/Intermediate proficiency level. */
    MEDIUM("Médio"),

    /** Low/Beginning proficiency level. */
    LOW("Baixo"),

    /** The skill was not assessed. */
    NOT_APPLICABLE("Não se Aplica")
}
