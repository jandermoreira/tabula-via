package edu.jm.classsupervision.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.classsupervision.model.Student

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long // Modificado para retornar o ID

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>) // Para restauração

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY name ASC")
    suspend fun getStudentsForClass(classId: Long): List<Student>

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Long): Student?

    @Query("SELECT * FROM students WHERE studentNumber = :studentNumber AND classId = :classId LIMIT 1")
    suspend fun getStudentByNumberInClass(studentNumber: String, classId: Long): Student?

    @Query("SELECT studentNumber FROM students WHERE classId = :classId")
    suspend fun getStudentNumbersForClass(classId: Long): List<String>
}
