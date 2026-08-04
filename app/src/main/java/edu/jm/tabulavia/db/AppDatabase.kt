/**
 * Main database configuration for the TabulaVia application.
 * Defines the schema, entities, and access points for the Room database.
 */
package edu.jm.tabulavia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.ActivityHighlightedSkillDao
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.dao.ClassDao
import edu.jm.tabulavia.dao.ClassSkillDao
import edu.jm.tabulavia.dao.EvidenceDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.dao.SkillAssessmentDao
import edu.jm.tabulavia.dao.SkillDao
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.AcademicClass
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.ActivityHighlightedSkill
import edu.jm.tabulavia.model.AssessmentSource
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.model.ClassSkill
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.SkillAssessment
import edu.jm.tabulavia.model.SkillLevel
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentSkill
import edu.jm.tabulavia.model.StudentStatus
import edu.jm.tabulavia.model.StudentTrackingState

@Database(
    entities = [
        AcademicClass::class,
        Student::class,
        ClassSession::class,
        AttendanceRecord::class,
        Activity::class,
        StudentSkill::class,
        GroupMember::class,
        ClassSkill::class,
        SkillAssessment::class,
        ActivityHighlightedSkill::class,
        Evidence::class,
        EvidenceScore::class
    ],
    version = 20,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to Class-related database operations.
     */
    abstract fun classDao(): ClassDao

    /**
     * Provides access to Student-related database operations.
     */
    abstract fun studentDao(): StudentDao

    /**
     * Provides access to Attendance-related database operations.
     */
    abstract fun attendanceDao(): AttendanceDao

    /**
     * Provides access to Activity-related database operations.
     */
    abstract fun activityDao(): ActivityDao

    /**
     * Provides access to StudentSkill-related database operations.
     */
    abstract fun skillDao(): SkillDao

    /**
     * Provides access to Group membership database operations.
     */
    abstract fun groupMemberDao(): GroupMemberDao

    /**
     * Provides access to Class Skill mapping database operations.
     */
    abstract fun classSkillDao(): ClassSkillDao

    /**
     * Provides access to Skill Assessment database operations.
     */
    abstract fun skillAssessmentDao(): SkillAssessmentDao

    /**
     * Provides access to Highlighted Skill database operations within activities.
     */
    abstract fun activityHighlightedSkillDao(): ActivityHighlightedSkillDao

    /**
     * Provides access to Evidence and Score database operations.
     */
    abstract fun evidenceDao(): EvidenceDao
}

/**
 * Type converters to handle non-primitive types in Room.
 * Converts Enums to Strings for database storage and vice versa.
 */
class Converters {

    /**
     * Converts AttendanceStatus enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name

    /**
     * Converts String back to AttendanceStatus enum.
     */
    @androidx.room.TypeConverter
    fun toAttendanceStatus(value: String) = AttendanceStatus.valueOf(value)

    /**
     * Converts SkillLevel enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromSkillLevel(value: SkillLevel) = value.name

    /**
     * Converts String back to SkillLevel enum.
     */
    @androidx.room.TypeConverter
    fun toSkillLevel(value: String) = SkillLevel.valueOf(value)

    /**
     * Converts AssessmentSource enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromAssessmentSource(value: AssessmentSource) = value.name

    /**
     * Converts String back to AssessmentSource enum.
     */
    @androidx.room.TypeConverter
    fun toAssessmentSource(value: String) = AssessmentSource.valueOf(value)

    /**
     * Converts StudentStatus enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromStudentStatus(value: StudentStatus) = value.name

    /**
     * Converts String back to StudentStatus enum.
     */
    @androidx.room.TypeConverter
    fun toStudentStatus(value: String) = StudentStatus.valueOf(value)

    /**
     * Converts EvidenceType enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromEvidenceType(value: EvidenceType) = value.name

    /**
     * Converts String back to EvidenceType enum.
     */
    @androidx.room.TypeConverter
    fun toEvidenceType(value: String) = EvidenceType.valueOf(value)

    /**
     * Converts StudentTrackingState enum to String for storage.
     */
    @androidx.room.TypeConverter
    fun fromStudentTrackingState(value: StudentTrackingState) = value.name

    /**
     * Converts String back to StudentTrackingState enum.
     */
    @androidx.room.TypeConverter
    fun toStudentTrackingState(value: String) = StudentTrackingState.valueOf(value)
}

