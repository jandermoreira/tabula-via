package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "attendance_records",
    primaryKeys = ["sessionId", "studentId"], // Chave primária composta
    foreignKeys = [
        ForeignKey(entity = ClassSession::class, parentColumns = ["sessionId"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["studentId"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("sessionId"), 
        Index("studentId")
    ] // Índices adicionados para otimização
)
data class AttendanceRecord(
    val sessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus
)
