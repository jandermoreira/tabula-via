/**
 * Data Access Object for the Student entity.
 * Provides methods to interact with the 'students' table in the database.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.jm.tabulavia.model.Student

@Dao
interface StudentDao {

    /**
     * Inserts a single student into the database.
     * Replaces the existing entry if there is a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    /**
     * Inserts a list of students into the database.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>)

    /**
     * Updates an existing student record.
     */
    @Update
    suspend fun updateStudent(student: Student)

    /**
     * Retrieves all students for a given class ordered by name.
     */
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    suspend fun getStudentsForClass(classId: Long): List<Student>

    /**
     * Retrieves a specific student based on its unique identifier.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    /**
     * Retrieves a student by number within a specific class.
     */
    @Query("SELECT * FROM students WHERE studentNumber = :studentNumber AND classId = :classId LIMIT 1")
    suspend fun getStudentByNumberInClass(studentNumber: String, classId: Long): Student?

    /**
     * Retrieves all student numbers for a specific class.
     */
    @Query("SELECT studentNumber FROM students WHERE classId = :classId")
    suspend fun getStudentNumbersForClass(classId: Long): List<String>

    /**
     * Retrieves all students from the database.
     */
    @Query("SELECT * FROM students")
    suspend fun getAllStudents(): List<Student>
}