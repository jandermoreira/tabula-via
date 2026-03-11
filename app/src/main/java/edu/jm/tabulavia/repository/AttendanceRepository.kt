/**
 * Repository for managing attendance operations.
 * Handles local Room persistence and remote Firestore synchronization for class sessions and attendance records.
 */
package edu.jm.tabulavia.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

/**
 * Data class representing a class session and its attendance for Firestore synchronization.
 */
data class FirestoreSession(
    val sessionId: String = "",
    val classId: String = "",
    val timestamp: Long = 0L,
    val attendance: Map<String, String> = emptyMap()
)

/**
 * Represents the result of an attendance save operation.
 */
sealed class SaveAttendanceResult {
    data class Success(val sessionId: String) : SaveAttendanceResult()
    data class Error(val message: String) : SaveAttendanceResult()
}

/**
 * Repository class handling attendance data with cloud synchronization.
 */
class AttendanceRepository(private val attendanceDao: AttendanceDao) {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Saves attendance records locally and synchronizes with Firestore.
     */
    suspend fun saveAttendance(
        classId: String,
        timestamp: Long,
        attendanceMap: Map<String, AttendanceStatus>,
        editingSession: ClassSession? = null
    ): SaveAttendanceResult = withContext(Dispatchers.IO) {
        try {
            // Determine session identifier
            val sessionId = editingSession?.sessionId ?: UUID.randomUUID().toString()

            val session = ClassSession(
                sessionId = sessionId,
                classId = classId,
                timestamp = timestamp
            )

            // Persist session locally
            attendanceDao.insertClassSession(session)

            // Clear previous records if editing an existing session
            if (editingSession != null) {
                attendanceDao.deleteAttendanceRecordsForSession(sessionId)
            }

            // Persist new attendance records locally
            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(sessionId = sessionId, studentId = studentId, status = status)
            }
            attendanceDao.insertAttendanceRecords(records)

            // Synchronize with remote storage
            syncSessionToFirestore(classId, sessionId, timestamp, attendanceMap)

            SaveAttendanceResult.Success(sessionId)
        } catch (e: Exception) {
            Log.e("AttendanceRepo", "Error saving attendance: ${e.message}")
            SaveAttendanceResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Pushes session and attendance data to the Firestore course hierarchy.
     */
    private suspend fun syncSessionToFirestore(
        classId: String,
        sessionId: String,
        timestamp: Long,
        attendanceMap: Map<String, AttendanceStatus>
    ) {
        val firestoreSession = FirestoreSession(
            sessionId = sessionId,
            classId = classId,
            timestamp = timestamp,
            attendance = attendanceMap.mapValues { it.value.name }
        )

        firestore.collection("courses")
            .document(classId)
            .collection("sessions")
            .document(sessionId)
            .set(firestoreSession)
            .await()
    }

    /**
     * Deletes a session locally and removes its remote document from Firestore.
     */
    suspend fun deleteSession(session: ClassSession) = withContext(Dispatchers.IO) {
        attendanceDao.deleteSession(session)
        try {
            firestore.collection("courses")
                .document(session.classId)
                .collection("sessions")
                .document(session.sessionId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("AttendanceRepo", "Failed to delete remote session: ${e.message}")
        }
    }

    /**
     * Retrieves all sessions for a specific class ordered by date.
     */
    suspend fun getClassSessions(classId: String): List<ClassSession> = withContext(Dispatchers.IO) {
        attendanceDao.getClassSessionsForClass(classId)
    }

    /**
     * Retrieves the attendance records for a specific session.
     */
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        attendanceDao.getAttendanceRecordsForSession(sessionId)
    }

    /**
     * Identifies the most recent session that occurred today.
     */
    fun getLastSessionToday(sessions: List<ClassSession>): ClassSession? {
        val today = Calendar.getInstance()
        return sessions.filter {
            val sessionDate = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            sessionDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    sessionDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        }.maxByOrNull { it.timestamp }
    }

    /**
     * Retrieves all class sessions across all courses.
     */
    suspend fun getAllSessions(): List<ClassSession> = withContext(Dispatchers.IO) {
        attendanceDao.getAllSessions()
    }

    /**
     * Retrieves all attendance records across all sessions.
     */
    suspend fun getAllRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        attendanceDao.getAllRecords()
    }

    /**
     * Bulk inserts multiple class sessions.
     */
    suspend fun insertAllSessions(sessions: List<ClassSession>) = withContext(Dispatchers.IO) {
        attendanceDao.insertAllSessions(sessions)
    }

    /**
     * Bulk inserts multiple attendance records.
     */
    suspend fun insertAllAttendanceRecords(records: List<AttendanceRecord>) = withContext(Dispatchers.IO) {
        attendanceDao.insertAttendanceRecords(records)
    }

    /**
     * Counts the total number of absences for a specific student.
     */
    suspend fun countStudentAbsences(studentId: String): Int = withContext(Dispatchers.IO) {
        attendanceDao.countStudentAbsences(studentId)
    }
}