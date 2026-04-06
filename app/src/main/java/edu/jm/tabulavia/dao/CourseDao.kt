/**
 * Data Access Object for the Course entity.
 * Provides methods to interact with the 'classes' table in the database.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.jm.tabulavia.model.Course
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    /**
     * Inserts a single course into the database.
     * Replaces the existing entry if there is a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    /**
     * Inserts a list of courses into the database.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<Course>)

    /**
     * Retrieves a specific course based on its unique identifier.
     * Returns null if no course is found with the given ID.
     */
    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getCourseById(classId: String): Course?

    /**
     * Updates a specific course based on its unique identifier.
     */
    @Update
    suspend fun updateCourse(course: Course)

    /**
     * Retrieves all courses from the database.
     * Results are ordered alphabetically by class name.
     */
    @Query("SELECT * FROM classes ORDER BY className ASC")
    fun getAllCoursesFlow(): Flow<List<Course>>

    /**
     * Retrieves all courses from the database as a one-time list.
     * Results are ordered alphabetically by class name.
     */
    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllCourses(): List<Course>

    /**
     * Deletes a course record from the database.
     *
     * @param clazz The course entity to be deleted. The entity must represent an existing
     * entry in the database for the deletion to succeed.
     */
    @Delete
    suspend fun deleteCourse(clazz: Course)
}