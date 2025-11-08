package edu.jm.classsupervision.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.jm.classsupervision.dao.ActivityDao
import edu.jm.classsupervision.dao.AttendanceDao
import edu.jm.classsupervision.dao.ClassDao
import edu.jm.classsupervision.dao.SkillDao
import edu.jm.classsupervision.dao.StudentDao
import edu.jm.classsupervision.model.*

@Database(
    entities = [
        Class::class, 
        Student::class, 
        ClassSession::class,
        AttendanceRecord::class,
        Activity::class,
        StudentSkill::class // Adicionada a nova entidade
    ], 
    version = 3, // Versão incrementada para 3
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun classDao(): ClassDao
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
