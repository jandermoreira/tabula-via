/**
 * Location.kt
 *
 * Defines the current placement of a student within the grouping screen.
 */

package edu.jm.tabulavia.model.grouping

/**
 * Represents the location where a student is currently assigned.
 */
sealed interface Location {
    /**
     * The student is not assigned to any group.
     */
    object Unassigned : Location

    /**
     * The student is assigned to a specific group.
     *
     * @property groupId The unique identifier of the group.
     */
    data class Group(val groupId: Int) : Location
}
