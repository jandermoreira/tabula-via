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
    val skillAssessments: List<SkillAssessment> = emptyList(),
    val courseSkills: List<CourseSkill> = emptyList(),
    val activityHighlightedSkills: List<ActivityHighlightedSkill> = emptyList()
)