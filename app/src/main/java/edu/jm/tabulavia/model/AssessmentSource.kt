/**
 * AssessmentSource.kt
 *
 * Defines the various sources of skill assessments within the application.
 */

package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Represents the origin of a skill assessment.
 *
 * @property displayName The Portuguese name of the source for UI display.
 */
@Serializable
enum class AssessmentSource(val displayName: String) {
    /** Assessment performed by the teacher. */
    PROFESSOR_OBSERVATION("Observação do Professor"),

    /** Assessment performed by the student about themselves. */
    SELF_ASSESSMENT("Autoavaliação"),

    /** Assessment performed by a peer student. */
    PEER_ASSESSMENT("Avaliação entre Pares")
}
