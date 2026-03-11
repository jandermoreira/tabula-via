/**
 * Repository for course management, activities, and group formations.
 * Handles local Room persistence and Firestore synchronization using persistent String IDs.
 */
package edu.jm.tabulavia.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.CourseDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.Course
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.Student
import kotlinx.coroutines.tasks.await

class CourseRepository(
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

    // ----------------- COURSE -----------------

    /**
     * Fetches all courses available in the local database.
     */
    suspend fun getAllCourses(): List<Course> = courseDao.getAllCourses()

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

    // ----------------- ACTIVITY -----------------

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
     * Persists a single activity record locally and synchronizes with Firestore.
     */
    suspend fun insertActivity(activity: Activity, uid: String) {
        activityDao.insert(activity)

        userCoursesRef(uid)
            .document(activity.classId)
            .collection("activities")
            .document(activity.activityId)
            .set(activity)
            .await()
    }

    /**
     * Bulk inserts a list of activities into the local database.
     */
    suspend fun insertAllActivities(activities: List<Activity>) = activityDao.insertAll(activities)

    /**
     * Retrieves a specific activity by its persistent String identifier.
     */
    suspend fun getActivityById(activityId: String): Activity? = activityDao.getActivityById(activityId)

    // ----------------- GROUPS -----------------

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