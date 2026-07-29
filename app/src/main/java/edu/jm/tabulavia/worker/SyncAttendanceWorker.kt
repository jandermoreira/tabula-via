/**
 * Worker responsible for synchronizing a specific attendance session with Firestore.
 * Ensures that local attendance data is reflected in the cloud, handling intermittent connectivity.
 */
package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.repository.FirestoreSession
import kotlinx.coroutines.tasks.await

class SyncAttendanceWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the background synchronization logic for an attendance session.
     * Fetches the session and its records from the local database and uploads them to Firestore.
     */
    override suspend fun doWork(): Result {
        val userEmail = inputData.getString("USER_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()
        val sessionId = inputData.getString("SESSION_ID") ?: return Result.failure()

        val database = DatabaseProvider.getDatabase(applicationContext)
        val attendanceDao = database.attendanceDao()
        val firestore = Firebase.firestore

        return try {
            val session = attendanceDao.getAllSessions().find { it.sessionId == sessionId }
                ?: return Result.success()

            val records = attendanceDao.getAttendanceRecordsForSession(sessionId)
            
            val firestoreSession = FirestoreSession(
                sessionId = session.sessionId,
                classId = session.classId,
                timestamp = session.timestamp,
                attendance = records.associate { it.studentId to it.status.name }
            )

            firestore.collection("users")
                .document(userEmail)
                .collection("classes")
                .document(classId)
                .collection("sessions")
                .document(sessionId)
                .set(firestoreSession)
                .await()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to e.message))
            }
        }
    }

    companion object {
        /**
         * Creates input data for this worker.
         *
         * @param email The email of the authenticated user.
         * @param classId The ID of the class the session belongs to.
         * @param sessionId The ID of the session to sync.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(email: String, classId: String, sessionId: String) = workDataOf(
            "USER_ID" to email,
            "CLASS_ID" to classId,
            "SESSION_ID" to sessionId
        )
    }
}
