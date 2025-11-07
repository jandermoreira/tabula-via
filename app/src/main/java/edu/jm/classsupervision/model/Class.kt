package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "classes")
data class Class(
    @PrimaryKey(autoGenerate = true)
    val classId: Long = 0,
    val className: String,
    val academicYear: String,
    val period: String,
    val numberOfClasses: Int = 0 // Novo campo para número de aulas
)
