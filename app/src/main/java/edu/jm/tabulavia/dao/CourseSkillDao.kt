/**
 * Data Access Object for course-level skill definitions.
 * Manages the blueprint of competencies required for each course.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.CourseSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseSkillDao {

    /**
     * Inserts a list of skills for a course.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseSkills(skills: List<CourseSkill>)

    /**
     * Retrieves all skills associated with a specific course.
     */
    @Query("SELECT * FROM course_skills WHERE courseId = :courseId")
    suspend fun getSkillsForCourse(courseId: String): List<CourseSkill>

    /**
     * Removes all skills associated with a specific course.
     */
    @Query("DELETE FROM course_skills WHERE courseId = :courseId")
    suspend fun clearSkillsForCourse(courseId: String)

    /**
     * Deletes a single course skill mapping.
     */
    @Delete
    suspend fun deleteCourseSkill(skill: CourseSkill)

    /**
     * Retrieves all course skill mappings.
     */
    @Query("SELECT * FROM course_skills")
    suspend fun getAllCourseSkills(): List<CourseSkill>

    /**
     * Retrieves a reactive flow of all skills associated with a specific course.
     */
    @Query("SELECT * FROM course_skills WHERE courseId = :courseId")
    fun getSkillsForCourseFlow(courseId: String): Flow<List<CourseSkill>>
}