/**
 * Data transfer object used for database backup and restoration.
 * Encapsulates all application entities into a single serializable structure.
 */
package edu.jm.tabulavia.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Data transfer object for a single class backup.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ClassBackup(
    /** The class being backed up. */
    @JsonNames("course")
    val clazz: AcademicClass,

    /** Students enrolled in this class. */
    val students: List<Student>,

    /** Sessions for this class. */
    val sessions: List<ClassSession>,

    /** Attendance records for all sessions in this class. */
    val attendance: List<AttendanceRecord>,

    /** Activities created for this class. */
    val activities: List<Activity>,

    /** Group membership for activities in this class. */
    val groupMembers: List<GroupMember> = emptyList(),

    /** Skills defined for this class. */
    val skills: List<ClassSkill> = emptyList(),

    /** Specific skills targeted or highlighted for each activity. */
    val highlightedSkills: List<ActivityHighlightedSkill> = emptyList(),

    /** Skill assessments for students in this class. */
    val assessments: List<SkillAssessment> = emptyList(),

    /** Consolidated skill levels for each student. */
    val studentSkills: List<StudentSkill> = emptyList(),

    /** Evidences recorded for this class. */
    val evidences: List<Evidence> = emptyList(),

    /** Scores associated with the evidences. */
    val evidenceScores: List<EvidenceScore> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BackupData(
    /** List of all classes registered in the system. */
    @JsonNames("courses")
    val classes: List<AcademicClass>,

    /** List of students across all classes. */
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

    /** Skills associated with specific classes. */
    val classSkills: List<ClassSkill> = emptyList(),

    /** Specific skills targeted or highlighted for each activity. */
    val activityHighlightedSkills: List<ActivityHighlightedSkill> = emptyList(),

    /** Consolidated skill levels for each student. */
    val studentSkills: List<StudentSkill> = emptyList(),

    /** Evidences recorded in the system. */
    val evidences: List<Evidence> = emptyList(),

    /** Scores associated with the evidences. */
    val evidenceScores: List<EvidenceScore> = emptyList()
)
