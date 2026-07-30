/**
 * DropTarget.kt
 *
 * Defines the possible targets where a student can be dropped during
 * a drag-and-drop operation within the grouping screen.
 */

package edu.jm.tabulavia.model.grouping

/**
 * Represents a valid destination for a dragged item.
 */
sealed interface DropTarget {
    /**
     * Target for returning a student to the list of unassigned students.
     */
    object Unassigned : DropTarget

    /**
     * Target for creating a new group with the dropped student.
     */
    object NewGroup : DropTarget

    /**
     * Target for adding a student to an already existing group.
     *
     * @property groupId The unique identifier of the target group.
     */
    data class ExistingGroup(val groupId: Int) : DropTarget
}
