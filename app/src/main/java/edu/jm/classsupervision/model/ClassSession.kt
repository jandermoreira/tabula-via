package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable // Anotação para serialização
@Entity(
    tableName = "class_sessions",
    foreignKeys = [ForeignKey(
        entity = Class::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("classId")]
)
data class ClassSession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val classId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
