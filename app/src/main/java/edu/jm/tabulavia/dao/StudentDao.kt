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
     * Retrieves all active and inactive students for a given class ordered by name.
     * Excludes students with CANCELLED status.
     * Returns a Flow to provide real-time updates when the table changes.
     */
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    fun getStudentsForClass(classId: String): Flow<List<Student>>

    /**
     * Retrieves all non-cancelled students for a given class as a list.
     */
    @Query("SELECT * FROM students WHERE classId = :classId AND status != 'CANCELLED' ORDER BY name ASC")
    suspend fun getStudentsForClassList(classId: String): List<Student>

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
     * Retrieves all student numbers for a specific class, excluding cancelled students.
     */
    @Query("SELECT studentNumber FROM students WHERE classId = :classId AND status != 'CANCELLED'")
    suspend fun getStudentNumbersForClass(classId: String): List<String>

    /**
     * Retrieves all non-cancelled students from the database as a flow.
     */
    @Query("SELECT * FROM students WHERE status != 'CANCELLED'")
    fun getAllStudentsFlow(): Flow<List<Student>>

    /**
     * Retrieves all non-cancelled students from the database as a list.
     */
    @Query("SELECT * FROM students WHERE status != 'CANCELLED'")
    fun getAllStudents(): List<Student>

    /**
     * Performs a physical deletion of a student record from the database.
     * Internal use only; soft delete (status = CANCELLED) is preferred for UI operations.
     */
    @Delete
    suspend fun deleteStudentPhysical(student: Student)
}