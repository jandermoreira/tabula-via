/**
 * Repository for course management, activities, and group formations.
 * Handles local Room persistence and Firestore synchronization using persistent String IDs.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.CourseDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.Course
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.worker.SyncActivityWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class CourseRepository(
    private val context: Context,
    private val courseDao: CourseDao,
    private val activityDao: ActivityDao,
    private val groupMemberDao: GroupMemberDao,
    private val firestore: FirebaseFirestore
) {

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
    suspend fun getCourseById(classId: String): Course? = courseDao.getCourseById(classId)

    /**
     * Saves a course locally and synchronizes it with Firestore.
     */
    suspend fun insertCourse(course: Course, uid: String): String {
        courseDao.insertCourse(course)

        userCoursesRef(uid)
            .document(course.classId)
            .set(course)
            .await()

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
    suspend fun getActivitiesForClass(classId: String): List<Activity> =
        activityDao.getActivitiesForClass(classId)

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
    suspend fun getActivityById(activityId: String): Activity? = activityDao.getActivityById(activityId)

    // Group Management Block

    /**
     * Records group assignments for an activity.
     */
    suspend fun persistGroups(activityId: String, groups: List<List<Student>>) {
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
    }

    /**
     * Retrieves all members and their group assignments for a specific activity.
     */
    suspend fun getGroupMembers(activityId: String): List<GroupMember> =
        groupMemberDao.getGroupMembersForActivity(activityId)

    /**
     * Fetches all group membership records from the local database.
     */
    suspend fun getAllGroupMembers(): List<GroupMember> = groupMemberDao.getAllGroupMembers()

    /**
     * Bulk inserts a list of group membership records.
     */
    suspend fun insertAllGroupMembers(members: List<GroupMember>) = groupMemberDao.insertAll(members)
}