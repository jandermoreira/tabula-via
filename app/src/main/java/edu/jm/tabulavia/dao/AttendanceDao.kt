/**
 * Data Access Object for attendance and class session entities.
 * Provides methods to interact with the 'class_sessions' and 'attendance_records' tables.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession

@Dao
interface AttendanceDao {

    /**
     * Inserts a class session into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassSession(session: ClassSession)

    /**
     * Inserts multiple class sessions into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSessions(sessions: List<ClassSession>)

    /**
     * Retrieves all sessions for a specific class ordered by timestamp.
     */
    @Query("SELECT * FROM class_sessions WHERE classId = :classId ORDER BY timestamp DESC")
    suspend fun getClassSessionsForClass(classId: String): List<ClassSession>

    /**
     * Retrieves all class sessions.
     */
    @Query("SELECT * FROM class_sessions")
    suspend fun getAllSessions(): List<ClassSession>

    /**
     * Deletes a class session.
     */
    @Delete
    suspend fun deleteSession(session: ClassSession)

    /**
     * Inserts attendance records for a session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    /**
     * Retrieves attendance records for a specific session.
     */
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getAttendanceRecordsForSession(sessionId: String): List<AttendanceRecord>

    /**
     * Retrieves all attendance records.
     */
    @Query("SELECT * FROM attendance_records")
    suspend fun getAllRecords(): List<AttendanceRecord>

    /**
     * Counts the number of absences for a student.
     */
    @Query("SELECT COUNT(*) FROM attendance_records WHERE studentId = :studentId AND status = :status")
    suspend fun countStudentAbsences(
        studentId: String,
        status: AttendanceStatus = AttendanceStatus.ABSENT
    ): Int

    /**
     * Deletes all attendance records associated with a specific session.
     */
    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteAttendanceRecordsForSession(sessionId: String)
}