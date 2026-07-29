/**
 * Worker responsible for deleting a specific skill document from Firestore.
 * Ensures that local deletions are reflected in the cloud even with intermittent connectivity.
 */
package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class SyncDeleteSkillWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    /**
     * Executes the deletion of the specified document in Firestore.
     */
    override suspend fun doWork(): Result {
        val userEmail = inputData.getString("USER_ID") ?: return Result.failure()
        val classId = inputData.getString("CLASS_ID") ?: return Result.failure()
        val firestoreId = inputData.getString("FIRESTORE_ID") ?: return Result.failure()

        val firestore = Firebase.firestore

        return try {
            // Reference to the specific document using the UUID generated at creation
            val documentReference = firestore.collection("users")
                .document(userEmail)
                .collection("classes")
                .document(classId)
                .collection("skills")
                .document(firestoreId)

            documentReference.delete().await()

            Result.success()
        } catch (error: Exception) {
            // Retries the deletion in case of network failure
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        /**
         * Creates input data for this worker.
         *
         * @param email The email of the authenticated user.
         * @param classId The ID of the class the skill belongs to.
         * @param firestoreId The unique Firestore identifier of the skill.
         * @return A [androidx.work.Data] object containing the worker's input.
         */
        fun buildInputData(email: String, classId: String, firestoreId: String) = androidx.work.workDataOf(
            "USER_ID" to email,
            "CLASS_ID" to classId,
            "FIRESTORE_ID" to firestoreId
        )
    }
}