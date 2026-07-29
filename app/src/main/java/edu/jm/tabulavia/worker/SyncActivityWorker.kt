package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.db.DatabaseProvider
import kotlinx.coroutines.tasks.await

/**
 * Worker responsible for synchronizing a specific activity with Firestore.
 * Ensures that data is eventually uploaded even if the network is unstable.
 */
class SyncActivityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the background synchronization logic.
     */
    override suspend fun doWork(): Result {
        val activityId = inputData.getString("ACTIVITY_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()
        val email = inputData.getString("USER_ID") ?: return Result.failure()

        val db = DatabaseProvider.getDatabase(applicationContext)
        val activityDao = db.activityDao()
        val firestore = Firebase.firestore

        return try {
            // Fetch the most recent local data to ensure consistency
            val activity = activityDao.getActivityById(activityId) ?: return Result.success()

            firestore.collection("users")
                .document(email)
                .collection("classes")
                .document(classId)
                .collection("activities")
                .document(activityId)
                .set(activity)
                .await()

            Result.success()
        } catch (e: Exception) {
            // Retries the operation if the attempt count is low
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        /**
         * Creates input data for this worker.
         *
         * @param email The email of the authenticated user.
         * @param classId The ID of the class the activity belongs to.
         * @param activityId The ID of the activity to sync.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(email: String, classId: String, activityId: String) = workDataOf(
            "USER_ID" to email,
            "CLASS_ID" to classId,
            "ACTIVITY_ID" to activityId
        )
    }
}