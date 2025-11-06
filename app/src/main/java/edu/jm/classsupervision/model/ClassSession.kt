package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_sessions",
    foreignKeys = [ForeignKey(
        entity = Class::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("classId")] // Índice adicionado para otimização
)
data class ClassSession(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0,
    val classId: Long,
    val timestamp: Long = System.currentTimeMillis() // Armazena a data e hora da aula
)
