/**
 * Repository for course management, activities, and group formations.
 * Handles the persistence of group memberships, course metadata, and backup operations.
 */
package edu.jm.tabulavia.repository

import edu.jm.tabulavia.dao.ActivityDao
import edu.jm.tabulavia.dao.CourseDao
import edu.jm.tabulavia.dao.GroupMemberDao
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.Course
import edu.jm.tabulavia.model.GroupMember
import edu.jm.tabulavia.model.Student

class CourseRepository(
    private val courseDao: CourseDao,
    private val activityDao: ActivityDao,
    private val groupMemberDao: GroupMemberDao
) {

    /**
     * Retrieves all courses stored in the local database.
     * @return List of all Course objects.
     */
    suspend fun getAllCourses(): List<Course> {
        return courseDao.getAllCourses()
    }

    /**
     * Fetches a specific course by its unique identifier.
     * @param classId The course identifier.
     * @return The Course object or null if not found.
     */
    suspend fun getCourseById(classId: Long): Course? {
        return courseDao.getCourseById(classId)
    }

    /**
     * Persists a new course into the database.
     * @param course The Course object to insert.
     * @return The generated ID of the new course.
     */
    suspend fun insertCourse(course: Course): Long {
        return courseDao.insertCourse(course)
    }

    /**
     * Inserts multiple courses at once, typically used during database restoration.
     * @param courses List of Course objects to insert.
     */
    suspend fun insertAllCourses(courses: List<Course>) {
        courseDao.insertAll(courses)
    }

    /**
     * Retrieves all activities associated with a specific class.
     * @param classId The class identifier.
     * @return List of Activity objects.
     */
    suspend fun getActivitiesForClass(classId: Long): List<Activity> {
        return activityDao.getActivitiesForClass(classId)
    }

    /**
     * Retrieves all activities in the database for backup purposes.
     * @return List of all Activity objects.
     */
    suspend fun getAllActivities(): List<Activity> {
        return activityDao.getAllActivities()
    }

    /**
     * Inserts a new activity record.
     * @param activity The Activity object to insert.
     * @return The generated ID of the new activity.
     */
    suspend fun insertActivity(activity: Activity): Long {
        return activityDao.insert(activity)
    }

    /**
     * Inserts multiple activities at once, used during database restoration.
     * @param activities List of Activity objects to insert.
     */
    suspend fun insertAllActivities(activities: List<Activity>) {
        activityDao.insertAll(activities)
    }

    /**
     * Retrieves an activity by its ID.
     * @param activityId The activity identifier.
     * @return The Activity object or null if not found.
     */
    suspend fun getActivityById(activityId: Long): Activity? {
        return activityDao.getActivityById(activityId)
    }

    /**
     * Clears and replaces group members for a specific activity.
     * Used when saving manually edited or automatically generated groups.
     * @param activityId The activity identifier.
     * @param groups List of groups, each group being a list of Student objects.
     */
    suspend fun persistGroups(activityId: Long, groups: List<List<Student>>) {
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
     * Loads the saved group structure for an activity.
     * @param activityId The activity identifier.
     * @return List of GroupMember records.
     */
    suspend fun getGroupMembers(activityId: Long): List<GroupMember> {
        return groupMemberDao.getGroupMembersForActivity(activityId)
    }

    /**
     * Retrieves all group members for backup purposes.
     * @return List of all GroupMember objects.
     */
    suspend fun getAllGroupMembers(): List<GroupMember> {
        return groupMemberDao.getAllGroupMembers()
    }

    /**
     * Inserts multiple group members at once, used during database restoration.
     * @param members List of GroupMember objects to insert.
     */
    suspend fun insertAllGroupMembers(members: List<GroupMember>) {
        groupMemberDao.insertAll(members)
    }
}