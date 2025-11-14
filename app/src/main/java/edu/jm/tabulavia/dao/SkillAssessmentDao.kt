package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import edu.jm.tabulavia.model.SkillAssessment
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillAssessmentDao {
    @Insert
    suspend fun insert(assessment: SkillAssessment)

    @Insert
    suspend fun insertAll(assessments: List<SkillAssessment>)

    @Query("SELECT * FROM skill_assessments WHERE studentId = :studentId AND skillName = :skillName ORDER BY timestamp DESC")
    fun getAssessmentsForSkill(studentId: Long, skillName: String): Flow<List<SkillAssessment>>

    @Query("SELECT * FROM skill_assessments WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAllAssessmentsForStudent(studentId: Long): Flow<List<SkillAssessment>>

    @Query("SELECT * FROM skill_assessments")
    fun getAllAssessments(): Flow<List<SkillAssessment>>
}
