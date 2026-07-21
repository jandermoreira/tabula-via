/**
 * Data Access Object for class-level skill definitions.
 * Manages the blueprint of competencies required for each class.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.ClassSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassSkillDao {

    /**
     * Inserts a list of skills for a class.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassSkills(skills: List<ClassSkill>)

    /**
     * Retrieves all skills associated with a specific class.
     */
    @Query("SELECT * FROM class_skills WHERE classId = :classId")
    suspend fun getSkillsForClass(classId: String): List<ClassSkill>

    /**
     * Removes all skills associated with a specific class.
     */
    @Query("DELETE FROM class_skills WHERE classId = :classId")
    suspend fun clearSkillsForClass(classId: String)

    /**
     * Deletes a single class skill mapping.
     */
    @Delete
    suspend fun deleteClassSkill(skill: ClassSkill)

    /**
     * Retrieves all class skill mappings.
     */
    @Query("SELECT * FROM class_skills")
    suspend fun getAllClassSkills(): List<ClassSkill>

    /**
     * Retrieves a reactive flow of all skills associated with a specific class.
     */
    @Query("SELECT * FROM class_skills WHERE classId = :classId")
    fun getSkillsForClassFlow(classId: String): Flow<List<ClassSkill>>
}