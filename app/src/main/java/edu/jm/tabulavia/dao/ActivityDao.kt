package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Activity
import edu.jm.tabulavia.model.ActivityHighlightedSkill

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<Activity>)

    @Query("SELECT * FROM activities WHERE activityId = :activityId")
    suspend fun getActivityById(activityId: Long): Activity?

    @Query("SELECT * FROM activities WHERE classId = :classId ORDER BY dueDate DESC")
    suspend fun getActivitiesForClass(classId: Long): List<Activity>

    @Query("SELECT * FROM activities")
    suspend fun getAllActivities(): List<Activity>
}

@Dao
interface ActivityHighlightedSkillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ActivityHighlightedSkill>)

    @Query("DELETE FROM activity_highlighted_skills WHERE activityId = :activityId")
    suspend fun clearForActivity(activityId: Long)

    @Query(
        """
        SELECT skillName 
        FROM activity_highlighted_skills 
        WHERE activityId = :activityId
        ORDER BY skillName ASC
        """
    )
    suspend fun getHighlightedSkillNamesForActivity(activityId: Long): List<String>

    @Query("SELECT * FROM activity_highlighted_skills")
    suspend fun getAll(): List<ActivityHighlightedSkill>
}