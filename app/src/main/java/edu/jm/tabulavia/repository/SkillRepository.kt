/**
 * Repository for managing class skills.
 * Provides local data access via Room and real-time synchronization with Firestore.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.ClassSkillDao
import edu.jm.tabulavia.model.ClassSkill
import edu.jm.tabulavia.model.SkillAssessment
import edu.jm.tabulavia.utils.shouldNotifySync
import edu.jm.tabulavia.worker.SyncClassSkillWorker
import edu.jm.tabulavia.worker.SyncDeleteSkillWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository responsible only for ClassSkill operations.
 *
 * Features:
 * - Observe skills for a class via Flow (backed by Room)
 * - Insert skills locally and sync to Firestore in background
 * - Delete skills locally and from Firestore
 * - Listen to Firestore changes to keep local data in sync across devices
 */
class SkillRepository(
    private val classSkillDao: ClassSkillDao,
    private val skillAssessmentDao: edu.jm.tabulavia.dao.SkillAssessmentDao,
    private val firestore: FirebaseFirestore,
    private val applicationContext: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    // Active Firestore listeners per class ID
    private val classSkillsListeners = mutableMapOf<String, () -> Unit>()

    // ---------- Observation ----------

    /**
     * Returns a Flow that emits the list of skills for a given class.
     * The Flow is updated automatically whenever the local database changes.
     */
    fun getSkillsFlowForClass(classId: String): Flow<List<ClassSkill>> =
        classSkillDao.getSkillsForClassFlow(classId)

    /**
     * Returns the current list of skills for a class (one-shot).
     */
    suspend fun getSkillsForClass(classId: String): List<ClassSkill> =
        classSkillDao.getSkillsForClass(classId)

    /**
     * Retrieves all assessments for a specific student.
     */
    suspend fun getAssessmentsForStudent(studentId: String): List<SkillAssessment> =
        skillAssessmentDao.getAssessmentsForStudentList(studentId)

    // ---------- Real-time sync from Firestore ----------

    /**
     * Starts listening to real-time changes for skills of a specific class.
     * Any change in Firestore will be reflected in the local database.
     *
     * @param email The authenticated user email.
     * @param classId The class unique identifier.
     * @param onSyncActivity Callback triggered when a remote change is detected.
     */
    fun startClassSkillsSync(email: String, classId: String, onSyncActivity: () -> Unit = {}) {
        stopListeningToClassSkills(classId)
        var isInitialSnapshot = true

        val listenerRegistration = firestore
            .collection("users/$email/classes/$classId/skills")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                        onSyncActivity()
                    }
                }
                isInitialSnapshot = false

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val skill = change.document.toObject(ClassSkill::class.java)
                            scope.launch {
                                classSkillDao.insertClassSkills(listOf(skill))
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val skill = change.document.toObject(ClassSkill::class.java)
                            scope.launch {
                                classSkillDao.deleteClassSkill(skill)
                            }
                        }
                    }
                }
            }

        classSkillsListeners[classId] = { listenerRegistration.remove() }
    }

    /**
     * Stops listening to changes for a specific class.
     */
    fun stopListeningToClassSkills(classId: String) {
        classSkillsListeners.remove(classId)?.invoke()
    }

    /**
     * Stops all active Firestore listeners.
     */
    fun stopAllListeners() {
        classSkillsListeners.values.forEach { it.invoke() }
        classSkillsListeners.clear()
    }

    /**
     * One-shot fetch of all skills for a specific class from Firestore.
     */
    suspend fun syncSkillsFromCloud(email: String, classId: String) {
        val snapshot = firestore
            .collection("users/$email/classes/$classId/skills")
            .get().await()
        val skills = snapshot.toObjects(ClassSkill::class.java).filterNotNull()
        if (skills.isNotEmpty()) {
            classSkillDao.insertClassSkills(skills)
        }
    }

    // ---------- Write operations (local + Firestore sync) ----------

    /**
     * Inserts a list of skills for a class with direct Firestore sync and background fallback.
     */
    suspend fun insertClassSkills(email: String, classId: String, skills: List<ClassSkill>) {
        // 1. Prepare skills with IDs
        val skillsWithIds = skills.map { skill ->
            if (skill.firestoreId == null) skill.copy(firestoreId = UUID.randomUUID().toString()) else skill
        }

        // 2. Local insert
        classSkillDao.insertClassSkills(skillsWithIds)

        // 3. Attempt direct remote sync
        try {
            val batch = firestore.batch()
            skillsWithIds.forEach { skill ->
                val docRef = firestore
                    .collection("users")
                    .document(email)
                    .collection("classes")
                    .document(classId)
                    .collection("skills")
                    .document(skill.firestoreId!!)
                batch.set(docRef, skill)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.w("SkillRepository", "Direct skill sync failed, falling back to Worker: ${e.message}")
            val syncRequest = OneTimeWorkRequestBuilder<SyncClassSkillWorker>()
                .setInputData(SyncClassSkillWorker.buildInputData(email, classId))
                .build()
            WorkManager.getInstance(applicationContext).enqueue(syncRequest)
        }
    }

    /**
     * Deletes a single class skill with direct Firestore sync and background fallback.
     */
    suspend fun deleteClassSkill(email: String, classId: String, skill: ClassSkill) {
        // 1. Local delete
        classSkillDao.deleteClassSkill(skill)

        // 2. Remote delete
        val firestoreId = skill.firestoreId ?: return
        try {
            firestore.collection("users")
                .document(email)
                .collection("classes")
                .document(classId)
                .collection("skills")
                .document(firestoreId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.w("SkillRepository", "Direct skill delete failed, falling back to Worker: ${e.message}")
            val syncRequest = OneTimeWorkRequestBuilder<SyncDeleteSkillWorker>()
                .setInputData(SyncDeleteSkillWorker.buildInputData(email, classId, firestoreId))
                .build()
            WorkManager.getInstance(applicationContext).enqueue(syncRequest)
        }
    }
}