/**
 * Group.kt
 *
 * Represents a logical grouping of students within the application.
 * Used primarily for collaborative activities and classroom organization.
 */

package edu.jm.tabulavia.model.grouping

import androidx.compose.runtime.snapshots.SnapshotStateList
import edu.jm.tabulavia.model.Student

/**
 * Data class representing a student group.
 *
 * @property id The unique identifier for the group.
 * @property students The observable list of students assigned to this group.
 */
data class Group(
    val id: Int,
    val students: SnapshotStateList<Student>
)
