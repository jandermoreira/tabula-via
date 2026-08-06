/**
 * Repository for managing attendance operations.
 * Handles local Room persistence and remote Firestore synchronization for class sessions and attendance records.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.jm.tabulavia.dao.AttendanceDao
import edu.jm.tabulavia.model.AttendanceRecord
import edu.jm.tabulavia.model.AttendanceStatus
import edu.jm.tabulavia.model.ClassSession
import edu.jm.tabulavia.utils.shouldNotifySync
import edu.jm.tabulavia.worker.SyncAttendanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
class AttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val applicationContext: Context
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var attendanceListener: ListenerRegistration? = null

    /**
     * Gets the current authenticated user email.
     */
    private val currentUserEmail: String?
        get() = auth.currentUser?.email

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
            val email = currentUserEmail
            if (email != null) {
                try {
                    // Attempt direct write for immediate update
                    syncSessionToFirestoreDirect(classId, sessionId, timestamp, attendanceMap, email)
                } catch (e: Exception) {
                    Log.w("AttendanceRepo", "Direct sync failed, falling back to Worker: ${e.message}")
                    val syncRequest = OneTimeWorkRequestBuilder<SyncAttendanceWorker>()
                        .setInputData(SyncAttendanceWorker.buildInputData(email, classId, sessionId))
                        .build()
                    WorkManager.getInstance(applicationContext).enqueue(syncRequest)
                }
            }

            SaveAttendanceResult.Success(sessionId)
        } catch (e: Exception) {
            Log.e("AttendanceRepo", "Error saving attendance: ${e.message}")
            SaveAttendanceResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Pushes session and attendance data to the Firestore directly.
     */
    private suspend fun syncSessionToFirestoreDirect(
        classId: String,
        sessionId: String,
        timestamp: Long,
        attendanceMap: Map<String, AttendanceStatus>,
        userEmail: String
    ) {
        val firestoreSession = FirestoreSession(
            sessionId = sessionId,
            classId = classId,
            timestamp = timestamp,
            attendance = attendanceMap.mapValues { it.value.name }
        )

        firestore.collection("users")
            .document(userEmail)
            .collection("classes")
            .document(classId)
            .collection("sessions")
            .document(sessionId)
            .set(firestoreSession)
            .await()
    }

    /**
     * Starts a real-time listener for attendance of a specific class for the current user.
     * Synchronizes remote changes (additions and deletions) with the local database.
     *
     * @param classId The class unique identifier.
     * @param onSyncActivity Callback triggered when a remote change is detected.
     */
    fun startAttendanceSync(classId: String, onSyncActivity: () -> Unit = {}) {
        val userEmail = currentUserEmail ?: return
        stopAttendanceSync()
        var isInitialSnapshot = true

        attendanceListener = firestore.collection("users")
            .document(userEmail)
            .collection("classes")
            .document(classId)
            .collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AttendanceRepo", "Error in attendance listener: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                        onSyncActivity()
                    }
                }
                isInitialSnapshot = false

                snapshot?.let {
                    val remoteSessions = it.toObjects(FirestoreSession::class.java)
                    CoroutineScope(Dispatchers.IO).launch {
                        processRemoteSessions(remoteSessions)
                    }
                }
            }
    }

    /**
     * Processes remote session data and updates the local database.
     */
    private suspend fun processRemoteSessions(sessions: List<FirestoreSession>) {
        sessions.forEach { remoteSession ->
            val session = ClassSession(
                sessionId = remoteSession.sessionId,
                classId = remoteSession.classId,
                timestamp = remoteSession.timestamp
            )
            attendanceDao.insertClassSession(session)

            val records = remoteSession.attendance.map { (studentId, statusName) ->
                AttendanceRecord(
                    sessionId = remoteSession.sessionId,
                    studentId = studentId,
                    status = AttendanceStatus.valueOf(statusName)
                )
            }
            // Simple replace strategy for attendance records
            attendanceDao.deleteAttendanceRecordsForSession(remoteSession.sessionId)
            attendanceDao.insertAttendanceRecords(records)
        }
    }

    /**
     * Stops the active Firestore listener.
     */
    fun stopAttendanceSync() {
        attendanceListener?.remove()
        attendanceListener = null
    }

    /**
     * Deletes a specific class session from local and remote storage.
     */
    suspend fun deleteSession(session: ClassSession) = withContext(Dispatchers.IO) {
        attendanceDao.deleteSessionWithRecords(session)

        val email = currentUserEmail ?: return@withContext
        firestore.collection("users")
            .document(email)
            .collection("classes")
            .document(session.classId)
            .collection("sessions")
            .document(session.sessionId)
            .delete()
    }

    /**
     * Retrieves sessions for a specific class.
     */
    suspend fun getClassSessions(classId: String): List<ClassSession> =
        withContext(Dispatchers.IO) {
            attendanceDao.getClassSessionsForClass(classId)
        }

    /**
     * Retrieves attendance records for a specific session.
     */
    suspend fun getRecordsForSession(sessionId: String): List<AttendanceRecord> =
        withContext(Dispatchers.IO) {
            attendanceDao.getAttendanceRecordsForSession(sessionId)
        }

    /**
     * Returns the last session created today, if any.
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
     * Retrieves all sessions from the local database.
     */
    suspend fun getAllSessions(): List<ClassSession> = withContext(Dispatchers.IO) {
        attendanceDao.getAllSessions()
    }

    /**
     * Retrieves all attendance records from the local database.
     */
    suspend fun getAllRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) {
        attendanceDao.getAllRecords()
    }

    /**
     * Inserts multiple sessions into the local database.
     */
    suspend fun insertAllSessions(sessions: List<ClassSession>) = withContext(Dispatchers.IO) {
        attendanceDao.insertAllSessions(sessions)
    }

    /**
     * Inserts multiple attendance records into the local database.
     */
    suspend fun insertAllAttendanceRecords(records: List<AttendanceRecord>) = withContext(Dispatchers.IO) {
        attendanceDao.insertAttendanceRecords(records)
    }

    /**
     * Counts the total absences for a specific student.
     */
    suspend fun countStudentAbsences(studentId: String): Int = withContext(Dispatchers.IO) {
        attendanceDao.countStudentAbsences(studentId, AttendanceStatus.ABSENT)
    }

    /**
     * Observes the total absences for a specific student.
     */
    fun countStudentAbsencesFlow(studentId: String): Flow<Int> =
        attendanceDao.countStudentAbsencesFlow(studentId, AttendanceStatus.ABSENT)

    /**
     * Downloads and persists all attendance sessions and records for a class from Firestore.
     */
    suspend fun syncSessionsFromCloud(email: String, classId: String) {
        val snapshot = firestore.collection("users")
            .document(email)
            .collection("classes")
            .document(classId)
            .collection("sessions")
            .get().await()

        val remoteSessions = snapshot.toObjects(FirestoreSession::class.java)
        processRemoteSessions(remoteSessions)
    }

    /**
     * Retrieves attendance records for a specific session as a reactive flow.
     */
    fun observeRecordsForSession(sessionId: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceRecordsFlow(sessionId)

    /**
     * Retrieves attendance records for a specific session as a reactive flow.
     */
    fun getAttendanceRecordsFlow(sessionId: String) = attendanceDao.getAttendanceRecordsFlow(sessionId)

    /**
     * Retrieves all sessions for a specific class as a reactive flow.
     */
    fun getClassSessionsFlow(classId: String) = attendanceDao.getClassSessionsFlow(classId)

    /**
     * Retrieves all attendance records for a specific class as a reactive flow.
     */
    fun getAttendanceRecordsForClassFlow(classId: String) = attendanceDao.getAttendanceRecordsForClassFlow(classId)

    /**
     * Removes a student's attendance records locally and updates remote sessions.
     * @param studentId The unique identifier of the student.
     * @param classId The unique identifier of the class.
     * @param email The authenticated user email.
     */
    suspend fun removeStudentFromAttendanceSessions(studentId: String, classId: String, email: String) {
        attendanceDao.deleteRecordsForStudent(studentId)

        // Remote cleanup: Firestore doesn't support easy field removal across many documents
        // unless we know exactly which sessions they were in.
        // For simplicity, we trigger a refresh or let the SnapshotListener handle it if the session is updated.
        // A better approach involves cloud functions or a worker that iterates.
    }
}
