/**
 * Data Access Object for SkillAssessment entity.
 * Provides methods to interact with the 'skill_assessments' table.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import edu.jm.tabulavia.model.SkillAssessment
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillAssessmentDao {

    /**
     * Inserts a single skill assessment into the database.
     */
    @Insert
    suspend fun insert(assessment: SkillAssessment)

    /**
     * Inserts multiple skill assessments into the database.
     */
    @Insert
    suspend fun insertAll(assessments: List<SkillAssessment>)

    /**
     * Retrieves all assessments for a specific student and skill ordered by timestamp descending.
     */
    @Query(
        "SELECT * FROM skill_assessments WHERE studentId = :studentId AND skillName = :skillName ORDER BY timestamp DESC"
    )
    fun getAssessmentsForSkill(studentId: String, skillName: String): Flow<List<SkillAssessment>>

    /**
     * Retrieves all assessments for a specific student ordered by timestamp descending.
     */
    @Query("SELECT * FROM skill_assessments WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAllAssessmentsForStudent(studentId: String): Flow<List<SkillAssessment>>

    /**
     * Retrieves all skill assessments in the database.
     */
    @Query("SELECT * FROM skill_assessments")
    fun getAllAssessmentsFlow(): Flow<List<SkillAssessment>>

    /**
     * Retrieves all skill assessments in the database.
     */
    @Query("SELECT * FROM skill_assessments")
    suspend fun getAllAssessments(): List<SkillAssessment>
}