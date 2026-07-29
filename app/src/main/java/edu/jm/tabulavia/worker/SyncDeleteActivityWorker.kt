/**
 * Worker responsible for synchronizing the deletion of an activity with Firestore.
 * Ensures that the remote document and its subcollections are removed even if the initial attempt fails.
 */
package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class SyncDeleteActivityWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the background deletion logic for an activity in Firestore.
     */
    override suspend fun doWork(): Result {
        val email = inputData.getString("USER_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()
        val activityId = inputData.getString("ACTIVITY_ID") ?: return Result.failure()

        return try {
            Firebase.firestore.collection("users")
                .document(email)
                .collection("classes")
                .document(classId)
                .collection("activities")
                .document(activityId)
                .delete()
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
         * @param classId The ID of the class.
         * @param activityId The ID of the activity to delete remotely.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(email: String, classId: String, activityId: String) = workDataOf(
            "USER_ID" to email,
            "CLASS_ID" to classId,
            "ACTIVITY_ID" to activityId
        )
    }
}
