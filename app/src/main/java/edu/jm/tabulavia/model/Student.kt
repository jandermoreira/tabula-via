/**
 * Student entity for the 'students' table.
 * Manages student data with a unique identifier to ensure isolation per class
 * and persistence across device reinstalls.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames

/**
 * Represents a student within an academic class.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = AcademicClass::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.NO_ACTION
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
    val studentId: String = "",

    /**
     * Full legal name of the student.
     */
    val name: String = "",

    /**
     * Name intended for UI display purposes.
     */
    val displayName: String = "",

    /**
     * Academic registration number or institutional ID.
     */
    val studentNumber: String = "",

    /**
     * Reference to the associated class ID.
     * Ensures the student belongs exclusively to one class instance.
     */
    @JsonNames("courseId")
    val classId: String = "",

    /**
     * Current enrollment status of the student.
     */
    val status: StudentStatus = StudentStatus.ACTIVE
) {
    /**
     * Returns the name to be displayed in the UI.
     * Uses displayName if it is not blank; otherwise, falls back to the full name.
     */
    @get:Exclude
    @Transient
    val effectiveName: String
        get() = displayName.ifBlank { name }
}