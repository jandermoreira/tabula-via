/**
 * Data transfer object used for database backup and restoration.
 * Encapsulates all application entities into a single serializable structure.
 */
package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

/**
 * Data transfer object for a single course backup.
 */
@Serializable
data class CourseBackup(
    /** The course being backed up. */
    val course: Course,

    /** Students enrolled in this course. */
    val students: List<Student>,

    /** Sessions for this course. */
    val sessions: List<ClassSession>,

    /** Attendance records for all sessions in this course. */
    val attendance: List<AttendanceRecord>,

    /** Activities created for this course. */
    val activities: List<Activity>,

    /** Group membership for activities in this course. */
    val groupMembers: List<GroupMember> = emptyList(),

    /** Skills defined for this course. */
    val skills: List<CourseSkill> = emptyList(),

    /** Skill assessments for students in this course. */
    val assessments: List<SkillAssessment> = emptyList()
)

@Serializable
data class BackupData(
    /** List of all courses registered in the system. */
    val courses: List<Course>,

    /** List of students across all courses. */
    val students: List<Student>,

    /** Records of class sessions and meetings. */
    val classSessions: List<ClassSession>,

    /** Student attendance history for sessions. */
    val attendanceRecords: List<AttendanceRecord>,

    /** Evaluative activities and assignments. */
    val activities: List<Activity>,

    /** Mapping of students to their respective groups within activities. */
    val groupMembers: List<GroupMember> = emptyList(),

    /** Individual skill evaluation records. */
    val skillAssessments: List<SkillAssessment> = emptyList(),

    /** Skills associated with specific courses. */
    val courseSkills: List<CourseSkill> = emptyList(),

    /** Specific skills targeted or highlighted for each activity. */
    val activityHighlightedSkills: List<ActivityHighlightedSkill> = emptyList(),

    /** Consolidated skill levels for each student. */
    val studentSkills: List<StudentSkill> = emptyList()
)