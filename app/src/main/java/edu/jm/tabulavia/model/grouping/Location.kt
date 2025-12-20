package edu.jm.tabulavia.model.grouping

sealed interface Location {
    object Unassigned : Location
    data class Group(val groupId: Int) : Location
}
