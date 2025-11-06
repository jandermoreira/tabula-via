package edu.jm.classsupervision.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val classes: List<Class>,
    val students: List<Student>,
    val classSessions: List<ClassSession>,
    val attendanceRecords: List<AttendanceRecord>
)