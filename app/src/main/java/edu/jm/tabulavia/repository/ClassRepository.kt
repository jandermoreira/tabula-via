/**
 * Repository for class management, activities, students, and group formations.
 * Handles local Room persistence and Firestore synchronization using persistent String IDs.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.ClassDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.AcademicClass
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.utils.shouldNotifySync
import edu.jm.tabulavia.worker.SyncActivityWorker
import edu.jm.tabulavia.worker.SyncClassWorker
import edu.jm.tabulavia.worker.SyncDeleteActivityWorker
import edu.jm.tabulavia.worker.SyncDeleteClassWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ClassRepository(
    private val context: Context,
    private val classDao: ClassDao,
    private val studentDao: StudentDao,
    private val activityDao: ActivityDao,
    private val groupMemberDao: GroupMemberDao,
    private val firestore: FirebaseFirestore
) {
    private var classesListener: ListenerRegistration? = null
    private var studentsListener: ListenerRegistration? = null
    private var activitiesListener: ListenerRegistration? = null

    /**
     * Helper to get the Firestore collection reference for a specific user's classes.
     */
    private fun userClassesRef(email: String) = firestore.collection("users")
        .document(email)
        .collection("classes")

    /**
     * Helper to get the Firestore collection reference for students within a class.
     */
    private fun userStudentsRef(email: String, classId: String) = userClassesRef(email)
        .document(classId)
        .collection("students")

    // Class Management Block

    /**
     * Exposes the stream of classes from the local database.
     */
    fun getAllClassesFlow(): Flow<List<AcademicClass>> = classDao.getAllClassesFlow()

    /**
     * Retrieves a single class by its persistent String identifier.
     */
    suspend fun getClassById(classId: String): AcademicClass? = classDao.getClassById(classId)

    /**
     * Saves a class locally and triggers an immediate Firestore sync with background fallback.
     */
    suspend fun insertClass(academicClass: AcademicClass, email: String): String {
        // Immediate local persistence
        classDao.insertClass(academicClass)

        try {
            // Direct write to Firestore for instant propagation
            userClassesRef(email)
                .document(academicClass.classId)
                .set(academicClass)
                .await()
        } catch (e: Exception) {
            Log.w("ClassRepository", "Direct class sync failed, falling back to Worker: ${e.message}")
            val syncRequest = OneTimeWorkRequestBuilder<SyncClassWorker>()
                .setInputData(SyncClassWorker.buildInputData(email, academicClass.classId))
                .build()
            WorkManager.getInstance(context).enqueue(syncRequest)
        }

        return academicClass.classId
    }

    /**
     * Deletes a class locally and from Firestore with background fallback.
     */
    suspend fun deleteClass(academicClass: AcademicClass, email: String) {
        withContext(Dispatchers.IO) {
            classDao.deleteClass(academicClass)
            try {
                userClassesRef(email)
                    .document(academicClass.classId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w("ClassRepository", "Direct class delete failed, falling back to Worker: ${e.message}")
                val syncWorkRequest = OneTimeWorkRequestBuilder<SyncDeleteClassWorker>()
                    .setInputData(SyncDeleteClassWorker.buildInputData(email, academicClass.classId))
                    .build()
                WorkManager.getInstance(context).enqueue(syncWorkRequest)
            }
        }
    }

    /**
     * Bulk inserts a list of classes into the local database.
     */
    suspend fun insertAllClasses(classes: List<AcademicClass>) = classDao.insertAll(classes)

    // Student Management Block

    /**
     * Retrieves all students associated with a specific class via String ID.
     */
    fun getStudentsForClass(classId: String): Flow<List<Student>> =
        studentDao.getStudentsForClass(classId)

    /**
     * Retrieves all students associated with a specific class as a list.
     */
    suspend fun getStudentsForClassList(classId: String): List<Student> =
        studentDao.getStudentsForClassList(classId)

    /**
     * Fetches every student stored in the local database.
     */
    suspend fun getAllStudents(): List<Student> = studentDao.getAllStudents()

    /**
     * Inserts a student record locally and syncs with Firestore.
     */
    suspend fun insertStudent(student: Student, email: String) {
        studentDao.insertStudent(student)

        userStudentsRef(email, student.classId)
            .document(student.studentId)
            .set(student)
            .await()
    }

    /**
     * Bulk inserts a list of students into local database and syncs to Firestore using batch write.
     */
    suspend fun insertAllStudents(students: List<Student>, email: String) {
        if (students.isEmpty()) return

        studentDao.insertAll(students)

        val batch = firestore.batch()
        students.forEach { student ->
            val docRef = userStudentsRef(email, student.classId).document(student.studentId)
            batch.set(docRef, student)
        }
        batch.commit().await()
    }

    /**
     * Bulk inserts a list of students into the local database only.
     */
    suspend fun insertAllStudentsLocal(students: List<Student>) = studentDao.insertAll(students)

    // Activity Management Block

    /**
     * Retrieves all activities associated with a specific class via String ID.
     */
    fun getActivitiesForClass(classId: String): Flow<List<Activity>> =
        activityDao.getActivitiesForClass(classId)

    /**
     * Retrieves all activities associated with a specific class as a list.
     */
    suspend fun getActivitiesForClassList(classId: String): List<Activity> =
        activityDao.getActivitiesForClassList(classId)

    /**
     * Fetches every activity stored in the local database.
     */
    suspend fun getAllActivities(): List<Activity> = activityDao.getAllActivities()

    /**
     * Persists a single activity record locally and attempts direct sync to Firestore.
     */
    suspend fun insertActivity(activity: Activity, email: String) {
        activityDao.insert(activity)

        try {
            userClassesRef(email)
                .document(activity.classId)
                .collection("activities")
                .document(activity.activityId)
                .set(activity)
                .await()
        } catch (e: Exception) {
            Log.w("ClassRepository", "Direct activity sync failed, falling back to Worker: ${e.message}")
            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncActivityWorker>()
                .setInputData(SyncActivityWorker.buildInputData(email, activity.classId, activity.activityId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "sync_activity_${activity.activityId}",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }
    }

    /**
     * Deletes an activity locally and from Firestore with background fallback.
     */
    suspend fun deleteActivity(activity: Activity, email: String) {
        withContext(Dispatchers.IO) {
            activityDao.delete(activity)
            try {
                userClassesRef(email)
                    .document(activity.classId)
                    .collection("activities")
                    .document(activity.activityId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w("ClassRepository", "Direct activity delete failed, falling back to Worker: ${e.message}")
                val syncWorkRequest = OneTimeWorkRequestBuilder<SyncDeleteActivityWorker>()
                    .setInputData(SyncDeleteActivityWorker.buildInputData(email, activity.classId, activity.activityId))
                    .build()
                WorkManager.getInstance(context).enqueue(syncWorkRequest)
            }
        }
    }

    /**
     * Bulk inserts a list of activities into the local database.
     */
    suspend fun insertAllActivities(activities: List<Activity>) = activityDao.insertAll(activities)

    /**
     * Retrieves a specific activity by its persistent String identifier.
     */
    suspend fun getActivityById(activityId: String): Activity? =
        activityDao.getActivityById(activityId)

    // Group Management Block

    /**
     * Records group assignments for an activity.
     */
    suspend fun persistGroups(
        activityId: String,
        groups: List<List<Student>>
    ) {
        // Persist group members to the local Room database
        groupMemberDao.clearGroupMembersForActivity(activityId)
        val groupMembers = groups.flatMapIndexed { groupIndex, studentList ->
            studentList.map { student ->
                GroupMember(
                    activityId = activityId,
                    studentId = student.studentId,
                    groupNumber = groupIndex + 1
                )
            }
        }
        groupMemberDao.insertGroupMembers(groupMembers)

        // Synchronize group data with Firestore in the background
        val currentUser = Firebase.auth.currentUser
        val email = currentUser?.email ?: throw IllegalStateException("User not logged in. Cannot persist groups.")

        try {
            val activity = activityDao.getActivityById(activityId)
            val classId = activity?.classId ?: throw IllegalStateException("Activity not found locally: $activityId")

            val groupDocumentRef = userClassesRef(email)
                .document(classId)
                .collection("activities")
                .document(activityId)
                .collection("groups")
                .document("groupData")

            val groupsMap = groups.mapIndexed { index, studentList ->
                "group_${index + 1}" to studentList.map { it.studentId }
            }.toMap()

            val documentData = mapOf(
                "groups" to groupsMap,
                "lastUpdated" to Timestamp.now()
            )

            groupDocumentRef.set(documentData).await()
            Log.d("ClassRepository", "Groups synchronized with Firestore for activity: $activityId")
        } catch (e: Exception) {
            Log.e("ClassRepository", "Error preparing groups for Firestore sync: ${e.message}", e)
            throw e
        }
    }

    /**
     * Retrieves all members and their group assignments for a specific activity as a list.
     */
    suspend fun getGroupMembersList(activityId: String): List<GroupMember> =
        groupMemberDao.getGroupMembersForActivityList(activityId)

    /**
     * Retrieves all members and their group assignments for a specific activity as a Flow.
     */
    fun getGroupMembers(activityId: String): Flow<List<GroupMember>> =
        groupMemberDao.getGroupMembersForActivity(activityId)

    /**
     * Fetches all group membership records from the local database.
     */
    suspend fun getAllGroupMembers(): List<GroupMember> = groupMemberDao.getAllGroupMembers()

    /**
     * Bulk inserts a list of group membership records.
     */
    suspend fun insertAllGroupMembers(members: List<GroupMember>) =
        groupMemberDao.insertAll(members)

    // Synchronization Block

    /**
     * Fetches all classes from Firestore and updates the local database.
     */
    suspend fun syncClassesFromCloud(email: String) {
        try {
            val snapshot = userClassesRef(email).get().await()
            val classes = snapshot.toObjects(AcademicClass::class.java)

            if (classes.isNotEmpty()) {
                classDao.insertAll(classes)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * One-shot fetch of all activities for a specific class from Firestore.
     */
    suspend fun syncActivitiesFromCloud(email: String, classId: String) {
        val snapshot = userClassesRef(email)
            .document(classId)
            .collection("activities")
            .get().await()
        val activities = snapshot.toObjects(Activity::class.java).filterNotNull()
        if (activities.isNotEmpty()) {
            activityDao.insertAll(activities)
        }
    }

    /**
     * Starts a real-time listener for the user's classes in Firestore.
     * Uses documentChanges to synchronize additions, updates, and deletions with Room.
     *
     * @param email The authenticated user email.
     * @param onSyncActivity Callback triggered when a remote change is detected.
     */
    fun startClassesSync(email: String, onSyncActivity: () -> Unit = {}) {
        stopClassesSync()
        var isInitialSnapshot = true

        classesListener = userClassesRef(email).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ClassRepository", "Firestore listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                    onSyncActivity()
                }
            }
            isInitialSnapshot = false

            snapshot?.documentChanges?.forEach { change ->
                val clazz = change.document.toObject(AcademicClass::class.java)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                classDao.insertClass(clazz)
                            }
                            DocumentChange.Type.REMOVED -> {
                                classDao.deleteClass(clazz)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ClassRepository", "Local database sync failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Stops the real-time listener to release resources.
     */
    fun stopClassesSync() {
        classesListener?.remove()
        classesListener = null
    }

    /**
     * Starts a real-time listener for students of a specific class.
     *
     * @param email The authenticated user email.
     * @param classId The unique identifier of the class.
     * @param onSyncActivity Callback triggered when a remote change is detected.
     */
    fun startStudentsSync(email: String, classId: String, onSyncActivity: () -> Unit = {}) {
        stopStudentsSync()
        var isInitialSnapshot = true

        studentsListener = userStudentsRef(email, classId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ClassRepository", "Firestore listener error: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                    onSyncActivity()
                }
            }
            isInitialSnapshot = false

            snapshot?.documentChanges?.forEach { change ->
                val docId = change.document.id
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val student = change.document.toObject(Student::class.java)
                                studentDao.insertStudent(student.copy(studentId = docId))
                            }
                            DocumentChange.Type.REMOVED -> {
                                studentDao.deleteStudent(Student(studentId = docId))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ClassRepository", "Local database student sync failed: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Stops the active students listener.
     */
    fun stopStudentsSync() {
        studentsListener?.remove()
        studentsListener = null
    }

    /**
     * Starts a real-time listener for activities of a specific class.
     *
     * @param email The authenticated user email.
     * @param classId The class unique identifier.
     * @param onSyncActivity Callback triggered when a remote change is detected.
     */
    fun startActivitiesSync(email: String, classId: String, onSyncActivity: () -> Unit = {}) {
        stopActivitiesSync()
        var isInitialSnapshot = true

        activitiesListener = userClassesRef(email)
            .document(classId)
            .collection("activities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ClassRepository", "Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                        onSyncActivity()
                    }
                }
                isInitialSnapshot = false

                snapshot?.documentChanges?.forEach { change ->
                    val docId = change.document.id
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            when (change.type) {
                                DocumentChange.Type.ADDED,
                                DocumentChange.Type.MODIFIED -> {
                                    val activity = change.document.toObject(Activity::class.java)
                                    activityDao.insert(activity.copy(activityId = docId))
                                }
                                DocumentChange.Type.REMOVED -> {
                                    activityDao.delete(Activity(activityId = docId))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ClassRepository", "Local database activity sync failed: ${e.message}")
                        }
                    }
                }
            }
    }

    /**
     * Stops the active activities listener.
     */
    fun stopActivitiesSync() {
        activitiesListener?.remove()
        activitiesListener = null
    }

    /**
     * Retrieves all classes from the local database as a one-time list.
     * Used for backup operations.
     */
    suspend fun getAllClasses(): List<AcademicClass> = classDao.getAllClasses()
}