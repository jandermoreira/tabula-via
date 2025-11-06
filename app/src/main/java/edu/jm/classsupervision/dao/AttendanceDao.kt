package edu.jm.classsupervision.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.classsupervision.model.AttendanceRecord
import edu.jm.classsupervision.model.ClassSession

@Dao
interface AttendanceDao {
    // --- Funções para Sessão de Aula ---
    @Insert
    suspend fun insertClassSession(session: ClassSession): Long

    @Query("SELECT * FROM class_sessions WHERE classId = :classId ORDER BY timestamp DESC")
    suspend fun getClassSessionsForClass(classId: Long): List<ClassSession>

    @Delete
    suspend fun deleteSession(session: ClassSession)

    // --- Funções para Registros de Frequência ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getAttendanceRecordsForSession(sessionId: Long): List<AttendanceRecord>
}
