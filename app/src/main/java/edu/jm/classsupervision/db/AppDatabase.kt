package edu.jm.classsupervision.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import edu.jm.classsupervision.dao.AttendanceDao
import edu.jm.classsupervision.dao.ClassDao
import edu.jm.classsupervision.dao.StudentDao
import edu.jm.classsupervision.model.Class
import edu.jm.classsupervision.model.Student
import edu.jm.classsupervision.model.ClassSession
import edu.jm.classsupervision.model.AttendanceRecord
import edu.jm.classsupervision.model.AttendanceStatus

@Database(
    entities = [
        Class::class, 
        Student::class, 
        ClassSession::class,
        AttendanceRecord::class
    ], 
    version = 1, 
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun classDao(): ClassDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    // A função abstrata clearAllTables() foi removida daqui.
}

class Converters {
    @androidx.room.TypeConverter
    fun fromAttendanceStatus(value: AttendanceStatus) = value.name

    @androidx.room.TypeConverter
    fun toAttendanceStatus(value: String) = AttendanceStatus.valueOf(value)
}
