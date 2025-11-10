package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.StudentSkill

@Dao
interface SkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSkills(skills: List<StudentSkill>)

    @Query("SELECT * FROM student_skills WHERE studentId = :studentId")
    suspend fun getSkillsForStudent(studentId: Long): List<StudentSkill>
}
