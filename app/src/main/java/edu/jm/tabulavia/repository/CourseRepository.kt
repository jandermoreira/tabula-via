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
     * Uses the pre-generated classId (UUID) as the document name.
     */
    suspend fun insertCourse(course: Course, uid: String): String {
        // Save locally - O classId já é o UUID gerado no CourseViewModel
        courseDao.insertCourse(course)

        // Sincronização direta: O ID do documento no Firestore é o próprio UUID do curso
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
     * Persists a single activity record locally.
     * Atividades também devem migrar para String ID se precisarem de sincronização robusta.
     */
    suspend fun insertActivity(activity: Activity): Long = activityDao.insert(activity)

    /**
     * Bulk inserts a list of activities into the local database.
     */
    suspend fun insertAllActivities(activities: List<Activity>) = activityDao.insertAll(activities)

    /**
     * Retrieves a specific activity by its unique local identifier.
     */
    suspend fun getActivityById(activityId: Long): Activity? = activityDao.getActivityById(activityId)

    // ----------------- GROUPS -----------------

    /**
     * Records group assignments for an activity.
     */
    suspend fun persistGroups(activityId: Long, groups: List<List<Student>>) {
        groupMemberDao.clearGroupMembersForActivity(activityId)

        val groupMembers = groups.flatMapIndexed { groupIndex, studentList ->
            studentList.map { student ->
                GroupMember(
                    activityId = activityId,
                    studentId = student.studentId, // studentId já é String (UUID)
                    groupNumber = groupIndex + 1
                )
            }
        }

        groupMemberDao.insertGroupMembers(groupMembers)
    }

    /**
     * Retrieves all members and their group assignments for a specific activity.
     */
    suspend fun getGroupMembers(activityId: Long): List<GroupMember> =
        groupMemberDao.getGroupMembersForActivity(activityId)

    /**
     * Fetches all group membership records from the local database.
     */
    suspend fun getAllGroupMembers(): List<GroupMember> = groupMemberDao.getAllGroupMembers()

    /**
     * Bulk inserts a list of group membership records.
     */
    suspend fun insertAllMembers(members: List<GroupMember>) = groupMemberDao.insertAll(members)
}