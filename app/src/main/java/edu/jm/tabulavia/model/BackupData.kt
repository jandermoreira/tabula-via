/**
 * Data transfer object used for database backup and restoration.
 * Encapsulates all application entities into a single serializable structure
 * to facilitate Firestore synchronization and local data persistence.
 */
package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

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

    /** consolidated skill levels for each student. */
    val studentSkills: List<StudentSkill> = emptyList()
)