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
        val uid = inputData.getString("USER_ID") ?: return Result.failure()

        val db = DatabaseProvider.getDatabase(applicationContext)
        val activityDao = db.activityDao()
        val firestore = Firebase.firestore

        return try {
            // Fetch the most recent local data to ensure consistency
            val activity = activityDao.getActivityById(activityId) ?: return Result.success()

            firestore.collection("users")
                .document(uid)
                .collection("courses")
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
}