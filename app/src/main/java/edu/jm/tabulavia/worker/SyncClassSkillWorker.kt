/**
 * Worker responsible for synchronizing class skills with Firestore.
 * Uses a batch operation to ensure all skills for a specific class are uploaded efficiently.
 * Designed to be triggered after local modifications or periodically as a fallback.
 */
package edu.jm.tabulavia.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import edu.jm.tabulavia.db.DatabaseProvider
import edu.jm.tabulavia.model.ClassSkill
import kotlinx.coroutines.tasks.await

/**
 * Syncs class skills for a specific user and class.
 *
 * Input data keys:
 * - USER_ID: Firebase Auth UID (required)
 * - CLASS_ID: target class ID (required)
 *
 * Returns [Result.success] if all skills were synced successfully.
 * Returns [Result.retry] on transient errors (up to 3 attempts).
 * Returns [Result.failure] on unrecoverable errors.
 */
class SyncClassSkillWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Extract required parameters
        val userId = inputData.getString("USER_ID") ?: return failure("Missing USER_ID")
        val classId = inputData.getString("CLASS_ID") ?: return failure("Missing CLASS_ID")

        val database = DatabaseProvider.getDatabase(applicationContext)
        val classSkillDao = database.classSkillDao()
        val firestore = FirebaseFirestore.getInstance()

        return try {
            // Fetch local skills for this class
            val localSkills = classSkillDao.getSkillsForClass(classId)

            if (localSkills.isEmpty()) {
                return Result.success()
            }

            // Separate skills that already have a Firestore ID from those that don't
            val (skillsWithId, skillsWithoutId) = localSkills.partition { !it.firestoreId.isNullOrBlank() }

            // Upload skills in a batch, generating Firestore IDs for new ones if necessary
            val batch = firestore.batch()
            val skillsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("classes")
                .document(classId)
                .collection("skills")

            // Process skills that already have an ID
            skillsWithId.forEach { skill ->
                skill.firestoreId?.let { id ->
                    batch.set(skillsCollection.document(id), skill)
                }
            }

            // Process skills without an ID (should not happen if local insertion always generates one, but safeguard)
            val newSkillsWithIds = skillsWithoutId.map { skill ->
                val newId = skill.firestoreId ?: java.util.UUID.randomUUID().toString()
                val updatedSkill = skill.copy(firestoreId = newId)
                batch.set(skillsCollection.document(newId), updatedSkill)
                updatedSkill
            }

            // Commit the batch
            batch.commit().await()

            // Update local database with any newly generated Firestore IDs
            if (newSkillsWithIds.isNotEmpty()) {
                classSkillDao.insertClassSkills(newSkillsWithIds)
            }

            Result.success()
        } catch (e: Exception) {
            // Retry up to 3 times on network issues or Firestore exceptions
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                failure("Sync failed after retries: ${e.message}")
            }
        }
    }

    /**
     * Helper to return a failure result with optional logging.
     */
    private fun failure(message: String): Result {
        // In production, you might want to log this to Crashlytics or similar
        return Result.failure(workDataOf("error" to message))
    }

    companion object {
        /** Creates input data for this worker */
        fun buildInputData(userId: String, classId: String) = workDataOf(
            "USER_ID" to userId,
            "CLASS_ID" to classId
        )
    }
}