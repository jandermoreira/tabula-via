/**
 * Data Access Object for StudentSkill entity.
 * Provides methods to interact with the 'student_skills' table.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.StudentSkill

@Dao
interface SkillDao {

    /**
     * Inserts or updates a list of student skills.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSkills(skills: List<StudentSkill>)

    /**
     * Retrieves all skills associated with a specific student.
     */
    @Query("SELECT * FROM student_skills WHERE studentId = :studentId")
    suspend fun getSkillsForStudent(studentId: String): List<StudentSkill>

    /**
     * Retrieves all student skills in the database.
     */
    @Query("SELECT * FROM student_skills")
    suspend fun getAllSkills(): List<StudentSkill>

    /**
     * Retrieves all skills for students in a specific course.
     */
    @Query("""
        SELECT ss.* FROM student_skills ss
        INNER JOIN students s ON ss.studentId = s.studentId
        WHERE s.classId = :classId
    """)
    suspend fun getSkillsForClass(classId: String): List<StudentSkill>
}