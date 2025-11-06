package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable // Anotação para serialização
@Entity(
    tableName = "attendance_records",
    primaryKeys = ["sessionId", "studentId"],
    foreignKeys = [
        ForeignKey(entity = ClassSession::class, parentColumns = ["sessionId"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["studentId"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("sessionId"), 
        Index("studentId")
    ]
)
data class AttendanceRecord(
    val sessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus
)
