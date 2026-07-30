/**
 * AttendanceStatus.kt
 *
 * Defines the possible states for student attendance.
 */

package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Represents the attendance status of a student for a specific session.
 *
 * @property displayName The Portuguese name of the status for UI display.
 */
@Serializable
enum class AttendanceStatus(val displayName: String) {
    /** Student was present. */
    PRESENT("Presente"),

    /** Student was absent. */
    ABSENT("Ausente"),

    /** Student was excused from the session. */
    EXCUSED("Dispensado")
}
