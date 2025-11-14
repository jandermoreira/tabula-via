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
        SkillAssessment::class // Adicionada a nova entidade
    ],
    version = 7, // Versão incrementada para 7
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun activityDao(): ActivityDao
    abstract fun skillDao(): SkillDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun courseSkillDao(): CourseSkillDao
    abstract fun skillAssessmentDao(): SkillAssessmentDao // Adicionado o novo DAO
}

class Converters {
    @androidx.room.TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name

    @androidx.room.TypeConverter
    fun toAttendanceStatus(value: String) = AttendanceStatus.valueOf(value)

    @androidx.room.TypeConverter
    fun fromSkillLevel(value: SkillLevel) = value.name

    @androidx.room.TypeConverter
    fun toSkillLevel(value: String) = SkillLevel.valueOf(value)

    @androidx.room.TypeConverter
    fun fromAssessmentSource(value: AssessmentSource) = value.name

    @androidx.room.TypeConverter
    fun toAssessmentSource(value: String) = AssessmentSource.valueOf(value)
}
