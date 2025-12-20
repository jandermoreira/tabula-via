package edu.jm.tabulavia.model.grouping

import androidx.compose.runtime.snapshots.SnapshotStateList
import edu.jm.tabulavia.model.Student

data class Group(
    val id: Int,
    val students: SnapshotStateList<Student>
)
