/**
 * Repository for course management, activities, and group formations.
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
import edu.jm.tabulavia.dao.CourseDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.Course
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.worker.SyncActivityWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ClassRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val activityDao: ActivityDao,
    private val groupMemberDao: GroupMemberDao,
    private val firestore: FirebaseFirestore
) {
    private var coursesListener: ListenerRegistration? = null
    private var studentsListener: ListenerRegistration? = null
    private var activitiesListener: ListenerRegistration? = null

    /**
     * Helper to get the Firestore collection reference for a specific user's courses.
     */
    private fun userCoursesRef(uid: String) = firestore.collection("users")
        .document(uid)
        .collection("courses")

    // Course Management Block

    /**
     * Exposes the stream of courses from the local database.
     */
    fun getAllCoursesFlow(): Flow<List<Course>> = courseDao.getAllCoursesFlow()

    /**
     * Retrieves a single course by its persistent String identifier.
     */
    suspend fun getCourseById(courseId: String): Course? = courseDao.getCourseById(courseId)

    /**
     * Saves a course locally and triggers a non-blocking cloud synchronization.
     */
    suspend fun insertCourse(course: Course, uid: String): String {
        // Immediate local persistence
        courseDao.insertCourse(course)

        // Asynchronous Firestore update managed by the SDK's internal queue
        userCoursesRef(uid)
            .document(course.classId)
            .set(course)

        return course.classId
    }

    /**
     * Bulk inserts a list of courses into the local database.
     */
    suspend fun insertAllCourses(courses: List<Course>) = courseDao.insertAll(courses)

    // Activity Management Block

    /**
     * Retrieves all activities associated with a specific course via String ID.
     */
    fun getActivitiesForClass(courseId: String): Flow<List<Activity>> =
        activityDao.getActivitiesForClass(courseId)

    /**
     * Fetches every activity stored in the local database.
     */
    suspend fun getAllActivities(): List<Activity> = activityDao.getAllActivities()

    /**
     * Persists a single activity record locally and schedules a background sync to Firestore.
     */
    suspend fun insertActivity(activity: Activity, uid: String) {
        activityDao.insert(activity)

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncActivityWorker>()
            .setInputData(
                workDataOf(
                    "ACTIVITY_ID" to activity.activityId,
                    "CLASS_ID" to activity.classId,
                    "USER_ID" to uid
                )
            )
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
        val uid = currentUser?.uid ?: throw IllegalStateException("User not logged in. Cannot persist groups.")

        try {
            val groupDocumentRef = Firebase.firestore.collection("users")
                .document(uid)
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

            groupDocumentRef.set(documentData)
                .addOnSuccessListener {
                    Log.d("CourseRepository", "Groups queued for Firestore sync for activity: $activityId")
                }
                .addOnFailureListener { e ->
                    Log.e("CourseRepository", "Failed to queue groups for Firestore sync (will retry): ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e("CourseRepository", "Error preparing groups for Firestore sync: ${e.message}", e)
            throw e
        }
    }

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
     * Fetches all courses from Firestore and updates the local database.
     */
    suspend fun syncCoursesFromCloud(uid: String) {
        try {
            val snapshot = userCoursesRef(uid).get().await()
            val courses = snapshot.toObjects(Course::class.java)

            if (courses.isNotEmpty()) {
                courseDao.insertAll(courses)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Starts a real-time listener for the user's courses in Firestore.
     * Uses documentChanges to synchronize additions, updates, and deletions with Room.
     *
     * @param uid The authenticated user ID.
     */
    fun startCoursesSync(uid: String) {
        stopCoursesSync()

        coursesListener = userCoursesRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ClassRepository", "Firestore listener error: ${error.message}")
                return@addSnapshotListener
            }

            snapshot?.documentChanges?.forEach { change ->
                val clazz = change.document.toObject(Course::class.java)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                courseDao.insertCourse(clazz)
                            }
                            DocumentChange.Type.REMOVED -> {
                                courseDao.deleteCourse(clazz)
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
    fun stopCoursesSync() {
        coursesListener?.remove()
        coursesListener = null
    }

//    /**
//     * Starts a real-time listener for students within a specific course.
//     */
//    fun startStudentsSync(uid: String, courseId: String) {
//        stopStudentsSync()
//
//        studentsListener = userCoursesRef(uid)
//            .document(courseId)
//            .collection("students")
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) return@addSnapshotListener
//
//                snapshot?.let {
//                    val students = it.toObjects(Student::class.java).filterNotNull()
//                    CoroutineScope(Dispatchers.IO).launch {
//                        studentDao.insertAll(students)
//                    }
//                }
//            }
//    }
//
//    /**
//     * Stops the real-time listener for students.
//     */
//    fun stopStudentsSync() {
//        studentsListener?.remove()
//        studentsListener = null
//    }

    /**
     * Starts a real-time listener for activities of a specific course.
     */
    fun startActivitiesSync(uid: String, courseId: String) {
        stopActivitiesSync()

        activitiesListener = userCoursesRef(uid)
            .document(courseId)
            .collection("activities")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let {
                    val activities = it.toObjects(Activity::class.java).filterNotNull()
                    CoroutineScope(Dispatchers.IO).launch {
                        activityDao.insertAll(activities)
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
     * Retrieves all courses from the local database as a one-time list.
     * Used for backup operations.
     */
    suspend fun getAllCourses(): List<Course> = courseDao.getAllCourses()
}
