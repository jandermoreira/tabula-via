/**
 * Student entity for the 'students' table.
 * Manages student data and its relationship with the Course entity.
 * Synchronized between Room local database and Firestore.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "students",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["classId"])]
)
data class Student(

    /**
     * Firestore document identifier.
     * Stored locally and used as the Room primary key.
     */
    @PrimaryKey
    val studentId: String,

    /**
     * Full legal name of the student.
     */
    val name: String,

    /**
     * Name intended for UI display purposes.
     */
    val displayName: String,

    /**
     * Academic registration number or institutional ID.
     */
    val studentNumber: String,

    /**
     * Reference to the associated class ID.
     */
    val classId: Long
)