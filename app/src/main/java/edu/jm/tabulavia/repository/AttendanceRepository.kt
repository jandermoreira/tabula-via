/**
 * Repository for attendance operations.
 * Provides methods to manage class sessions and attendance records.
 */
package edu.jm.tabulavia.repository

import android.util.Log
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Represents the result of a save attendance operation.
 */
sealed class SaveAttendanceResult {
    data class Success(val sessionId: Long) : SaveAttendanceResult()
    data class Error(val message: String) : SaveAttendanceResult()
}

class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    /**
     * Retrieves all class sessions for a given course.
     * @param classId The identifier of the course.
     * @return List of ClassSession objects.
     */
    suspend fun getClassSessions(classId: Long): List<ClassSession> {
        return attendanceDao.getClassSessionsForClass(classId)
    }

    /**
     * Retrieves attendance records for a specific session.
     * @param sessionId The identifier of the session.
     * @return List of AttendanceRecord objects.
     */
    suspend fun getRecordsForSession(sessionId: Long): List<AttendanceRecord> {
        return attendanceDao.getAttendanceRecordsForSession(sessionId)
    }

    /**
     * Counts the number of absences for a specific student.
     * @param studentId The student's identifier.
     * @return Total count of absences.
     */
    suspend fun countStudentAbsences(studentId: Long): Int {
        return attendanceDao.countStudentAbsences(studentId)
    }

    /**
     * Retrieves all class sessions from the database.
     * Used primarily for backup operations.
     * @return List of all ClassSession objects.
     */
    suspend fun getAllSessions(): List<ClassSession> {
        return attendanceDao.getAllSessions()
    }

    /**
     * Retrieves all attendance records from the database.
     * Used primarily for backup operations.
     * @return List of all AttendanceRecord objects.
     */
    suspend fun getAllRecords(): List<AttendanceRecord> {
        return attendanceDao.getAllRecords()
    }

    /**
     * Saves attendance data for a course session.
     * If editingSession is provided, the existing session is reused and its previous records are replaced.
     * Otherwise a new session is inserted.
     *
     * @param classId The course identifier.
     * @param timestamp The timestamp of the session.
     * @param attendanceMap Map of student IDs to attendance status.
     * @param editingSession Existing session when editing, null when creating a new one.
     * @return SaveAttendanceResult indicating success or failure.
     */
    suspend fun saveAttendance(
        classId: Long,
        timestamp: Long,
        attendanceMap: Map<Long, AttendanceStatus>,
        editingSession: ClassSession? = null
    ): SaveAttendanceResult = withContext(Dispatchers.IO) {
        try {
            // Reuse session ID if editing, otherwise insert a new session
            val sessionId = editingSession?.sessionId
                ?: attendanceDao.insertClassSession(
                    ClassSession(classId = classId, timestamp = timestamp)
                )

            // Remove previous records when editing an existing session
            if (editingSession != null) {
                attendanceDao.deleteAttendanceRecordsForSession(sessionId)
            }

            // Create attendance records from the map
            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(
                    sessionId = sessionId,
                    studentId = studentId,
                    status = status
                )
            }

            // Insert all updated records
            attendanceDao.insertAttendanceRecords(records)
            SaveAttendanceResult.Success(sessionId)
        } catch (e: Exception) {
            Log.e("AttendanceRepository", "Error saving attendance: ${e.message}", e)
            SaveAttendanceResult.Error("Falha ao salvar frequência: ${e.message}")
        }
    }

    /**
     * Deletes a class session and its associated attendance records.
     * @param session The session to delete.
     */
    suspend fun deleteSession(session: ClassSession) = withContext(Dispatchers.IO) {
        attendanceDao.deleteSession(session)
    }

    /**
     * Finds the last session that occurred today from a list of sessions.
     * @param sessions List of class sessions.
     * @return The latest session today, or null if none.
     */
    fun getLastSessionToday(sessions: List<ClassSession>): ClassSession? {
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val todayEnd = calendar.timeInMillis

        return sessions
            .filter { it.timestamp in todayStart..todayEnd }
            .maxByOrNull { it.timestamp }
    }

    /**
     * Inserts multiple class sessions in bulk (used during restore).
     * Delegates directly to the DAO's batch insert method.
     * @param sessions List of sessions to insert.
     */
    suspend fun insertAllSessions(sessions: List<ClassSession>) = withContext(Dispatchers.IO) {
        attendanceDao.insertAllSessions(sessions)
    }

    /**
     * Inserts multiple attendance records in bulk (used during restore).
     * Delegates directly to the DAO's batch insert method.
     * @param records List of records to insert.
     */
    suspend fun insertAllAttendanceRecords(records: List<AttendanceRecord>) =
        withContext(Dispatchers.IO) {
            attendanceDao.insertAttendanceRecords(records)
        }
}