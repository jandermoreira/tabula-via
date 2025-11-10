package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Course

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>) // Para restauração

    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllCourses(): List<Course>

    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getCourseById(classId: Long): Course?
}
