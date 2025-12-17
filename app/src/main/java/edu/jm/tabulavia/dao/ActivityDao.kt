package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Activity

@Dao
interface ActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity): Long

    suspend fun insertActivity(activity: Activity) = insert(activity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<Activity>)

    @Query("SELECT * FROM activities WHERE activityId = :activityId")
    suspend fun getActivityById(activityId: Long): Activity?

    @Query("SELECT * FROM activities WHERE classId = :classId ORDER BY dueDate DESC")
    suspend fun getActivitiesForClass(classId: Long): List<Activity>

    @Query("SELECT * FROM activities")
    suspend fun getAllActivities(): List<Activity>
}