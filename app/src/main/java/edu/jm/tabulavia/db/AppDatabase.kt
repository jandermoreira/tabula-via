/**
 * Main database configuration for the TabulaVia application.
 * Defines the schema, entities, and access points for the Room database.
 */
package edu.jm.tabulavia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.jm.tabulavia.dao.*
import edu.jm.tabulavia.model.*

@Database(
    entities = [
        Course::class,
        Student::class,
        ClassSession::class,
        AttendanceRecord::class,
        Activity::class,
        StudentSkill::class,
        GroupMember::class,
        CourseSkill::class,
        SkillAssessment::class,
        ActivityHighlightedSkill::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Provides access to Course-related database operations.
     */
    abstract fun courseDao(): CourseDao

    /**
     * Provides access to Student-related database operations.
     */
    abstract fun studentDao(): StudentDao

//    /**
//     * Provides access to ClassSession-related database operations.
//     */
//    abstract fun classSessionDao(): ClassSessionDao

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
     * Provides access to Course Skill mapping database operations.
     */
    abstract fun courseSkillDao(): CourseSkillDao

    /**
     * Provides access to Skill Assessment database operations.
     */
    abstract fun skillAssessmentDao(): SkillAssessmentDao

    /**
     * Provides access to Highlighted Skill database operations within activities.
     */
    abstract fun activityHighlightedSkillDao(): ActivityHighlightedSkillDao
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
}