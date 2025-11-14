package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "skill_assessments")
data class SkillAssessment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val skillName: String, // Ou um skillId se você tiver uma tabela de Skills
    val level: SkillLevel,
    val source: AssessmentSource,
    val timestamp: Long = System.currentTimeMillis(),
    val assessorId: Long? = null // Opcional: para saber qual colega avaliou
)
