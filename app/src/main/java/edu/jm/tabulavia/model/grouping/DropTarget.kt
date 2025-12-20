package edu.jm.tabulavia.model.grouping

sealed interface DropTarget {
    object Unassigned : DropTarget
    object NewGroup : DropTarget
    data class ExistingGroup(val groupId: Int) : DropTarget
}
