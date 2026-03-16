/**
 * Repository for managing course skills.
 * Provides local data access via Room and real-time synchronization with Firestore.
 */
package edu.jm.tabulavia.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.CourseSkillDao
import edu.jm.tabulavia.model.CourseSkill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository responsible only for CourseSkill operations.
 *
 * Features:
 * - Observe skills for a course via Flow (backed by Room)
 * - Insert skills locally and sync to Firestore in background
 * - Delete skills locally and from Firestore
 * - Listen to Firestore changes to keep local data in sync across devices
 */
class SkillRepository(
    private val courseSkillDao: CourseSkillDao,
    private val firestore: FirebaseFirestore,
    private val scope: CoroutineScope
) {
    // Active Firestore listeners per course ID
    private val courseSkillsListeners = mutableMapOf<String, () -> Unit>()

    // ---------- Observation ----------

    /**
     * Returns a Flow that emits the list of skills for a given course.
     * The Flow is updated automatically whenever the local database changes.
     */
    fun getSkillsFlowForCourse(courseId: String): Flow<List<CourseSkill>> =
        courseSkillDao.getSkillsForCourseFlow(courseId)

    /**
     * Returns the current list of skills for a course (one-shot).
     */
    suspend fun getSkillsForCourse(courseId: String): List<CourseSkill> =
        courseSkillDao.getSkillsForCourse(courseId)

    // ---------- Real-time sync from Firestore ----------

    /**
     * Starts listening to real-time changes for skills of a specific course.
     * Any change in Firestore will be reflected in the local database.
     */
    fun startListeningToCourseSkills(uid: String, courseId: String) {
        stopListeningToCourseSkills(courseId) // remove any existing listener

        val listenerRegistration = firestore
            .collection("users/$uid/courses/$courseId/skills")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Log error if needed (optional)
                    return@addSnapshotListener
                }
                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val skill = change.document.toObject(CourseSkill::class.java)
                            scope.launch {
                                courseSkillDao.insertCourseSkills(listOf(skill))
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val skill = change.document.toObject(CourseSkill::class.java)
                            scope.launch {
                                courseSkillDao.deleteCourseSkill(skill)
                            }
                        }
                    }
                }
            }

        courseSkillsListeners[courseId] = { listenerRegistration.remove() }
    }

    /**
     * Stops listening to changes for a specific course.
     */
    fun stopListeningToCourseSkills(courseId: String) {
        courseSkillsListeners.remove(courseId)?.invoke()
    }

    /**
     * Stops all active Firestore listeners.
     */
    fun stopAllListeners() {
        courseSkillsListeners.values.forEach { it.invoke() }
        courseSkillsListeners.clear()
    }

    // ---------- Write operations (local + Firestore sync) ----------

    /**
     * Inserts a list of skills for a course.
     *
     * Steps:
     * 1. Inserts skills into the local Room database (fast, suspending).
     * 2. Launches a background coroutine to upsert each skill into Firestore.
     *    If a skill lacks a firestoreId, one is generated and the local record is updated.
     */
    suspend fun insertCourseSkills(uid: String, courseId: String, skills: List<CourseSkill>) {
        // 1. Local insert
        courseSkillDao.insertCourseSkills(skills)

        // 2. Background sync to Firestore
        scope.launch {
            try {
                skills.forEach { skill ->
                    val firestoreId = skill.firestoreId ?: UUID.randomUUID().toString()
                    val skillWithId = if (skill.firestoreId == null) {
                        skill.copy(firestoreId = firestoreId)
                    } else {
                        skill
                    }
                    val docRef = firestore
                        .collection("users/$uid/courses/$courseId/skills")
                        .document(firestoreId)
                    docRef.set(skillWithId).await()

                    // If we generated a new firestoreId, update the local record to have it
                    if (skill.firestoreId == null) {
                        courseSkillDao.insertCourseSkills(listOf(skillWithId))
                    }
                }
            } catch (e: Exception) {
                // Handle error (e.g., log, notify via a central error channel)
            }
        }
    }

    /**
     * Deletes a single course skill.
     *
     * Steps:
     * 1. Deletes locally from Room.
     * 2. Launches a background coroutine to delete the corresponding Firestore document
     *    (if it has a firestoreId).
     */
    suspend fun deleteCourseSkill(uid: String, courseId: String, skill: CourseSkill) {
        // 1. Local delete
        courseSkillDao.deleteCourseSkill(skill)

        // 2. Background Firestore delete
        scope.launch {
            try {
                skill.firestoreId?.let { firestoreId ->
                    firestore.collection("users/$uid/courses/$courseId/skills")
                        .document(firestoreId)
                        .delete()
                        .await()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}