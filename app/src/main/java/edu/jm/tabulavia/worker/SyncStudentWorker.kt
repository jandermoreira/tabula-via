/**
 * Worker responsible for synchronizing a specific student with Firestore.
 * Ensures that local student data is reflected in the cloud, handling intermittent connectivity.
 */

package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.db.DatabaseProvider
import kotlinx.coroutines.tasks.await

class SyncStudentWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the background synchronization logic for a student.
     * Fetches the student from the local database and uploads it to Firestore.
     */
    override suspend fun doWork(): Result {
        // Extract required parameters from input data
        val userId = inputData.getString("USER_ID") ?: return failure("Missing USER_ID")
        val classId = inputData.getString("CLASS_ID") ?: return failure("Missing CLASS_ID")
        val studentId = inputData.getString("STUDENT_ID") ?: return failure("Missing STUDENT_ID")

        // Initialize database and Firestore instances
        val database = DatabaseProvider.getDatabase(applicationContext)
        val studentDao = database.studentDao()
        val firestore = Firebase.firestore

        return try {
            // Fetch the most recent local data for the student
            val student = studentDao.getStudentById(studentId)
                ?: return Result.success() // If student not found locally, it might have been deleted

            // Reference to the specific student document in Firestore
            val documentReference = firestore.collection("users")
                .document(userId)
                .collection("courses")
                .document(classId)
                .collection("students")
                .document(studentId)

            // Upload the student data to Firestore
            documentReference.set(student).await()

            Result.success()
        } catch (e: Exception) {
            // Retry the operation if the attempt count is low (e.g., network issues)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                // Log the failure message and return failure
                failure("Sync failed after retries for student $studentId: ${e.message}")
            }
        }
    }

    /**
     * Helper function to create a [Result.failure] with an associated error message.
     *
     * @param message The error message to include in the failure data.
     * @return A [Result.failure] instance.
     */
    private fun failure(message: String): Result {
        // In a real application, you might want to log this error to a crash reporting tool
        return Result.failure(workDataOf("error" to message))
    }

    companion object {
        /**
         * Creates input data for this worker.
         *
         * @param userId The ID of the authenticated user.
         * @param classId The ID of the course the student belongs to.
         * @param studentId The ID of the student to sync.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(userId: String, classId: String, studentId: String) = workDataOf(
            "USER_ID" to userId,
            "CLASS_ID" to classId,
            "STUDENT_ID" to studentId
        )
    }
}
