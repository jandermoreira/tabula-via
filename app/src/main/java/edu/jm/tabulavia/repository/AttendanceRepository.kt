/**
 * Repository for managing attendance operations.
 * Handles local Room persistence and remote Firestore synchronization for class sessions and attendance records.
 */
package edu.jm.tabulavia.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
    private val auth = FirebaseAuth.getInstance()
    private var attendanceListener: ListenerRegistration? = null

    /**
     * Gets the current authenticated user ID.
     */
    private val currentUserId: String?
        get() = auth.currentUser?.uid

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
            val sessionId = editingSession?.sessionId ?: UUID.randomUUID().toString()

            val session = ClassSession(
                sessionId = sessionId,
                classId = classId,
                timestamp = timestamp
            )

            // Local database operations
            attendanceDao.insertClassSession(session)

            if (editingSession != null) {
                attendanceDao.deleteAttendanceRecordsForSession(sessionId)
            }

            val records = attendanceMap.map { (studentId, status) ->
                AttendanceRecord(sessionId = sessionId, studentId = studentId, status = status)
            }
            attendanceDao.insertAttendanceRecords(records)

            // Firestore synchronization
            syncSessionToFirestore(classId, sessionId, timestamp, attendanceMap)

            SaveAttendanceResult.Success(sessionId)
        } catch (e: Exception) {
            Log.e("AttendanceRepo", "Error saving attendance: ${e.message}")
            SaveAttendanceResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Pushes session and attendance data to the Firestore course hierarchy using the authenticated user ID.
     */
    private fun syncSessionToFirestore(
        classId: String,
        sessionId: String,
        timestamp: Long,
        attendanceMap: Map<String, AttendanceStatus>
    ) {
        val userId = currentUserId ?: run {
            Log.e("AttendanceRepo", "User not authenticated for Firestore sync")
            return
        }

        val firestoreSession = FirestoreSession(
            sessionId = sessionId,
            classId = classId,
            timestamp = timestamp,
            attendance = attendanceMap.mapValues { it.value.name }
        )

        // Maps data to users/{userId}/courses/{classId}/sessions/{sessionId}
        firestore.collection("users")
            .document(userId)
            .collection("courses")
            .document(classId)
            .collection("sessions")
            .document(sessionId)
            .set(firestoreSession)
    }

    /**
     * Starts a real-time listener for attendance of a specific course for the current user.
     * Synchronizes remote changes (additions and deletions) with the local database.
     */
    fun startAttendanceSync(classId: String) {
        val userId = currentUserId ?: return
        stopAttendanceSync()

        attendanceListener = firestore.collection("users")
            .document(userId)
            .collection("courses")
            .document(classId)
            .collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.documentChanges?.forEach { change ->
                    val remote = change.document.toObject(FirestoreSession::class.java)

                    CoroutineScope(Dispatchers.IO).launch {
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                val session = ClassSession(
                                    sessionId = remote.sessionId,
                                    classId = remote.classId,
                                    timestamp = remote.timestamp
                                )
                                val records = remote.attendance.map { (studentId, statusName) ->
                                    AttendanceRecord(
                                        sessionId = remote.sessionId,
                                        studentId = studentId,
                                        status = AttendanceStatus.valueOf(statusName)
                                    )
                                }
                                attendanceDao.insertClassSession(session)
                                attendanceDao.insertAttendanceRecords(records)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                // Deletes locally when the document is removed from Firestore
                                val sessionToDelete = ClassSession(
                                    sessionId = remote.sessionId,
                                    classId = remote.classId,
                                    timestamp = remote.timestamp
                                )
                                attendanceDao.deleteSessionWithRecords(sessionToDelete)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Processes sessions retrieved from remote storage and updates local database.
     */
    private suspend fun processRemoteSessions(sessions: List<FirestoreSession>) {
        sessions.forEach { remote ->
            val session = ClassSession(
                sessionId = remote.sessionId,
                classId = remote.classId,
                timestamp = remote.timestamp
            )
            val records = remote.attendance.map { (studentId, statusName) ->
                AttendanceRecord(
                    sessionId = remote.sessionId,
                    studentId = studentId,
                    status = AttendanceStatus.valueOf(statusName)
                )
            }
            attendanceDao.insertClassSession(session)
            attendanceDao.insertAttendanceRecords(records)
        }
    }

    /**
     * Stops the active attendance listener.
     */
    fun stopAttendanceSync() {
        attendanceListener?.remove()
        attendanceListener = null
    }

    /**
     * Deletes a session locally using a transaction and removes its remote document from Firestore.
     */
    suspend fun deleteSession(session: ClassSession) = withContext(Dispatchers.IO) {
        val userId = currentUserId

        attendanceDao.deleteSessionWithRecords(session)

        if (userId != null) {
            firestore.collection("users")
                .document(userId)
                .collection("courses")
                .document(session.classId)
                .collection("sessions")
                .document(session.sessionId)
                .delete()
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

    /**
     * Returns a flow that emits the total count of absences for a specific student.
     */
    fun countStudentAbsencesFlow(studentId: String): Flow<Int> {
        return attendanceDao.countStudentAbsencesFlow(studentId)
    }

    /**
     * Retrieves attendance records for a specific session as a reactive flow.
     */
    fun getAttendanceRecordsFlow(sessionId: String) = attendanceDao.getAttendanceRecordsFlow(sessionId)

    /**
     * Retrieves all sessions for a specific class as a reactive flow.
     */
    fun getClassSessionsFlow(classId: String) = attendanceDao.getClassSessionsFlow(classId)
}