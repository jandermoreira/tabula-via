/**
 * Worker responsible for synchronizing a specific class with Firestore.
 * Ensures that local class data is reflected in the cloud, handling intermittent connectivity.
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

class SyncClassWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the background synchronization logic for a class.
     * Fetches the class from the local database and uploads it to Firestore.
     */
    override suspend fun doWork(): Result {
        val userEmail = inputData.getString("USER_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()

        val database = DatabaseProvider.getDatabase(applicationContext)
        val classDao = database.classDao()
        val firestore = Firebase.firestore

        return try {
            val academicClass = classDao.getClassById(classId)
                ?: return Result.success() // If class not found locally, it might have been deleted

            firestore.collection("users")
                .document(userEmail)
                .collection("classes")
                .document(classId)
                .set(academicClass)
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
         * @param classId The ID of the class to sync.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(email: String, classId: String) = workDataOf(
            "USER_ID" to email,
            "CLASS_ID" to classId
        )
    }
}
