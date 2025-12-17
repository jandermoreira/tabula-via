package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.ActivityHighlightedSkill

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
