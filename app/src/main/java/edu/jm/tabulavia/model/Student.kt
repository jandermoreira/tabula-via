/**
 * Student entity for the 'students' table.
 * Manages student data with a unique identifier to ensure isolation per course
 * and persistence across device reinstalls.
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
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class Student(

    /**
     * Unique identifier for the student.
     * Must be a UUID generated at creation or retrieved from Firestore.
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
     * Ensures the student belongs exclusively to one course instance.
     */
    val classId: String
)