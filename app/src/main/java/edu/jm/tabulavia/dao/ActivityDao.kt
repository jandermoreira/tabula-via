/**
 * Data Access Objects for Activity and ActivityHighlightedSkill entities.
 * Provides methods to interact with the 'activities' and
 * 'activity_highlighted_skills' tables.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.ActivityHighlightedSkill

@Dao
interface ActivityDao {

    /**
     * Inserts a single activity into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    /**
     * Inserts multiple activities into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<Activity>)

    /**
     * Retrieves an activity by its identifier.
     */
    @Query("SELECT * FROM activities WHERE activityId = :activityId")
    suspend fun getActivityById(activityId: Long): Activity?

    /**
     * Retrieves all activities for a specific class ordered by due date.
     */
    @Query("SELECT * FROM activities WHERE classId = :classId ORDER BY dueDate DESC")
    suspend fun getActivitiesForClass(classId: Long): List<Activity>

    /**
     * Retrieves all activities.
     */
    @Query("SELECT * FROM activities")
    suspend fun getAllActivities(): List<Activity>
}

@Dao
interface ActivityHighlightedSkillDao {

    /**
     * Inserts highlighted skills for activities.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ActivityHighlightedSkill>)

    /**
     * Removes all highlighted skills associated with an activity.
     */
    @Query("DELETE FROM activity_highlighted_skills WHERE activityId = :activityId")
    suspend fun clearForActivity(activityId: Long)

    /**
     * Retrieves the names of highlighted skills for an activity.
     */
    @Query(
        """
        SELECT skillName
        FROM activity_highlighted_skills
        WHERE activityId = :activityId
        ORDER BY skillName ASC
        """
    )
    suspend fun getHighlightedSkillNamesForActivity(activityId: Long): List<String>

    /**
     * Retrieves all highlighted skill mappings.
     */
    @Query("SELECT * FROM activity_highlighted_skills")
    suspend fun getAll(): List<ActivityHighlightedSkill>
}