package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.CourseSkill

@Dao
interface CourseSkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseSkills(skills: List<CourseSkill>)

    @Query("SELECT * FROM course_skills WHERE courseId = :courseId")
    suspend fun getSkillsForCourse(courseId: Long): List<CourseSkill>

    @Query("DELETE FROM course_skills WHERE courseId = :courseId")
    suspend fun clearSkillsForCourse(courseId: Long)

    @Delete
    suspend fun deleteCourseSkill(skill: CourseSkill)

    @Query("SELECT * FROM course_skills")
    suspend fun getAllCourseSkills(): List<CourseSkill>
}
