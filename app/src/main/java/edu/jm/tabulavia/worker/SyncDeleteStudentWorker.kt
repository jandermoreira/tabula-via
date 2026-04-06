/**
 * Worker responsible for deleting a student document from Firestore.
 * This worker ensures that remote data is synchronized even if the initial
 * deletion attempt occurs while the device is offline.
 */
package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncDeleteStudentWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the remote deletion logic.
     * Accesses the specific Firestore document path and performs a delete operation.
     */
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()
        val studentId = inputData.getString("STUDENT_ID") ?: return Result.failure()

        val firestore = FirebaseFirestore.getInstance()

        return try {
            // Reference to users/{userId}/courses/{classId}/students/{studentId}
            firestore.collection("users")
                .document(userId)
                .collection("courses")
                .document(classId)
                .collection("students")
                .document(studentId)
                .delete()
                .await()

            Result.success()
        } catch (e: Exception) {
            // Retries the operation in case of connectivity issues or transient errors
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }

    companion object {
        /**
         * Builds the input data required for the worker to identify the remote document.
         *
         * @param userId The authenticated user unique identifier.
         * @param classId The course unique identifier.
         * @param studentId The student unique identifier.
         * @return Data object containing the identifiers.
         */
        fun buildInputData(userId: String, classId: String, studentId: String) = workDataOf(
            "USER_ID" to userId,
            "CLASS_ID" to classId,
            "STUDENT_ID" to studentId
        )
    }
}