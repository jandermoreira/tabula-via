package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val courses: List<Course>,
    val students: List<Student>,
    val classSessions: List<ClassSession>,
    val attendanceRecords: List<AttendanceRecord>,
    val activities: List<Activity>,
    val groupMembers: List<GroupMember> = emptyList(),
    val studentSkills: List<StudentSkill> = emptyList()
)
