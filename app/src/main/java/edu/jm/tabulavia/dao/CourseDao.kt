/**
 * Data Access Object for the Course entity.
 * Provides methods to interact with the 'classes' table in the database.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.tabulavia.model.Course

@Dao
interface CourseDao {

    /**
     * Inserts a single course into the database.
     * Replaces the existing entry if there is a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    /**
     * Inserts a list of courses into the database.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    /**
     * Retrieves all courses from the database.
     * Results are ordered alphabetically by class name.
     */
    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllCourses(): List<Course>

    /**
     * Retrieves a specific course based on its unique identifier.
     * Returns null if no course is found with the given ID.
     */
    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getCourseById(classId: Long): Course?
}