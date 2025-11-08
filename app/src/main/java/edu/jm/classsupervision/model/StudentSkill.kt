package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.ForeignKey
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
    ]
)
data class StudentSkill(
    val studentId: Long,
    val skillName: String,
    val state: SkillState
)
