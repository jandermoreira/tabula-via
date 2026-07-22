/**
 * File: StudentStatus.kt
 * Description: Defines the possible enrollment statuses for a student.
 */

package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Enumeration representing the current status of a student's enrollment.
 */
@Serializable
enum class StudentStatus {
    /**
     * Student is currently active and participating in activities.
     */
    ACTIVE,

    /**
     * Student is temporarily inactive or on leave.
     */
    INACTIVE,

    /**
     * Student's enrollment has been permanently cancelled.
     */
    CANCELLED
}
