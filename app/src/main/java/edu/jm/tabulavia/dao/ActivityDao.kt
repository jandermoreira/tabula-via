/**
 * Data Access Objects for Activity and ActivityHighlightedSkill entities.
 * Provides methods to interact with the 'activities' and
 * 'activity_highlighted_skills' tables using String-based identifiers.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.ActivityHighlightedSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    /**
     * Inserts a single activity into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity)

    /**
     * Inserts multiple activities into the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<Activity>)

    /**
     * Retrieves an activity by its persistent String identifier.
     */
    @Query("SELECT * FROM activities WHERE activityId = :activityId")
    suspend fun getActivityById(activityId: String): Activity?

    /**
     * Retrieves all activities for a specific class ordered by timestamp.
     */
    @Query("SELECT * FROM activities WHERE classId = :classId ORDER BY timestamp DESC")
    fun getActivitiesForClass(classId: String): Flow<List<Activity>>

    /**
     * Retrieves all activities across all courses.
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
     * Removes all highlighted skills associated with a specific activity.
     */
    @Query("DELETE FROM activity_highlighted_skills WHERE activityId = :activityId")
    suspend fun clearForActivity(activityId: String)

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
    suspend fun getHighlightedSkillNamesForActivity(activityId: String): List<String>

    /**
     * Retrieves all highlighted skill mappings from the database.
     */
    @Query("SELECT * FROM activity_highlighted_skills")
    suspend fun getAll(): List<ActivityHighlightedSkill>
}