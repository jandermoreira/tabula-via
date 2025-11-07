package edu.jm.classsupervision.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.classsupervision.model.Activity

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: Activity)

    @Query("SELECT * FROM activities WHERE classId = :classId ORDER BY dueDate DESC")
    suspend fun getActivitiesForClass(classId: Long): List<Activity>
}
