package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class CourseSkill(
    val courseId: Long,
    val skillName: String
)
