/**
 * Data Access Object for the Student entity.
 * Provides methods to interact with the 'students' table in the database.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.jm.tabulavia.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {

    /**
     * Inserts a single student into the database.
     * Replaces the existing entry if there is a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

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
     * Returns a Flow to provide real-time updates when the table changes.
     */
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    fun getStudentsForClass(classId: String): Flow<List<Student>>

    /**
     * Retrieves a specific student based on its unique identifier.
     */
    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: String): Student?

    /**
     * Retrieves a student by number within a specific class.
     */
    @Query("SELECT * FROM students WHERE studentNumber = :studentNumber AND classId = :classId LIMIT 1")
    suspend fun getStudentByNumberInClass(studentNumber: String, classId: String): Student?

    /**
     * Retrieves all student numbers for a specific class.
     */
    @Query("SELECT studentNumber FROM students WHERE classId = :classId")
    suspend fun getStudentNumbersForClass(classId: String): List<String>

    /**
     * Retrieves all students from the database as a flow.
     */
    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<Student>>

    /**
     * Deletes a student record from the database.
     *
     * @param student The student entity to be deleted. This must represent an
     * existing entry in the database for the operation to succeed.
     */
    @Delete
    suspend fun deleteStudent(student: Student)
}