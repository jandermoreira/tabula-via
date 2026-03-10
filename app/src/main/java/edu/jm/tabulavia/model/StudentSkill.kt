/**
 * StudentSkill entity for the 'student_skills' table.
 * Represents the state of a specific skill for a given student.
 */

package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "student_skills",
    primaryKeys = ["studentId", "skillName"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class StudentSkill(

    /**
     * Identifier of the associated student.
     */
    val studentId: String,

    /**
     * Name of the skill being tracked.
     */
    val skillName: String,

    /**
     * Current state of the skill for the student.
     */
    val state: SkillState,

    /**
     * Firestore document identifier.
     */
    val firestoreId: String? = null
)

