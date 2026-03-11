/**
 * CourseSkill entity for the 'course_skills' table.
 * Represents the skills associated with a specific course.
 */

package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "course_skills",
    primaryKeys = ["courseId", "skillName"],
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["classId"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class CourseSkill(

    /**
     * Identifier of the associated course.
     */
    val courseId: String,

    /**
     * Name of the skill associated with the course.
     */
    val skillName: String,

    /**
     * Firestore document identifier.
     */
    val firestoreId: String? = null
)