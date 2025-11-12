package edu.jm.tabulavia.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.dao.CourseDao
import edu.jm.tabulavia.dao.SkillDao
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.*

@Database(
    entities = [
        Course::class, 
        Student::class, 
        ClassSession::class,
        AttendanceRecord::class,
        Activity::class,
        StudentSkill::class // Adicionada a nova entidade
    ], 
    version = 4, // Versão incrementada para 4
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun activityDao(): ActivityDao
    abstract fun skillDao(): SkillDao // Adicionado o novo DAO
}

class Converters {
    @androidx.room.TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name

    @androidx.room.TypeConverter
    fun toAttendanceStatus(value: String) = AttendanceStatus.valueOf(value)
    
    // Conversores para o novo SkillState
    @androidx.room.TypeConverter
    fun fromSkillState(value: SkillState) = value.name

    @androidx.room.TypeConverter
    fun toSkillState(value: String) = SkillState.valueOf(value)
}
