/**
 * Repository responsible for student-related database operations.
 * Provides methods for retrieving, adding, updating, and bulk inserting students.
 */
package edu.jm.tabulavia.repository

import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentRepository(private val studentDao: StudentDao) {

    /**
     * Retrieves all students belonging to a specific class.
     * @param classId The identifier of the class.
     * @return List of students in that class.
     */
    suspend fun getStudentsForClass(classId: Long): List<Student> {
        return studentDao.getStudentsForClass(classId)
    }

    /**
     * Retrieves all students from the database.
     * Used primarily for backup operations.
     * @return List of all students.
     */
    suspend fun getAllStudents(): List<Student> {
        return studentDao.getAllStudents()
    }

    /**
     * Fetches a single student by their unique database identifier.
     * @param studentId The student's ID.
     * @return The student object or null if not found.
     */
    suspend fun getStudentById(studentId: Long): Student? {
        return studentDao.getStudentById(studentId)
    }

    /**
     * Updates an existing student's information in the database.
     * @param student The student object with updated fields.
     */
    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    /**
     * Inserts multiple students into the database in a single transaction.
     * Used during database restoration.
     * @param students List of students to insert.
     */
    suspend fun insertAllStudents(students: List<Student>) {
        studentDao.insertAll(students)
    }

    /**
     * Attempts to add a new student after checking for existing student numbers in the class.
     * @param name The student's full name.
     * @param studentNumber The student's registration number.
     * @param classId The class to which the student belongs.
     * @return RepositoryResult indicating success or failure with a message.
     */
    suspend fun addStudent(
        name: String,
        studentNumber: String,
        classId: Long
    ): RepositoryResult {
        val existingStudent = studentDao.getStudentByNumberInClass(studentNumber, classId)

        return if (existingStudent == null) {
            val newStudent = Student(
                name = name,
                displayName = name,
                studentNumber = studentNumber,
                classId = classId
            )
            studentDao.insertStudent(newStudent)
            RepositoryResult(true, "Aluno '$name' adicionado com sucesso.")
        } else {
            RepositoryResult(false, "Erro: Aluno com matrícula '$studentNumber' já existe.")
        }
    }

    /**
     * Parses a raw string and performs batch insertion of students.
     * Skips students whose numbers are already registered in the class.
     * @param bulkText Multi-line string where each line contains "number name".
     * @param classId The target class identifier.
     * @return RepositoryResult with a summary message.
     */
    suspend fun addStudentsInBulk(
        bulkText: String,
        classId: Long
    ): RepositoryResult = withContext(Dispatchers.IO) {
        val existingNumbers = studentDao.getStudentNumbersForClass(classId).toSet()
        val ignoredStudents = mutableListOf<String>()
        var addedCount = 0

        bulkText.lines().forEach { line ->
            val parts = line.trim().split(Regex("\\s+"), 2)
            if (parts.size == 2) {
                val number = parts[0]
                val name = parts[1]
                if (number.isNotBlank() && name.isNotBlank()) {
                    if (existingNumbers.contains(number)) {
                        ignoredStudents.add(name)
                    } else {
                        val newStudent = Student(
                            name = name,
                            displayName = name,
                            studentNumber = number,
                            classId = classId
                        )
                        studentDao.insertStudent(newStudent)
                        addedCount++
                    }
                }
            }
        }

        val message = when {
            ignoredStudents.isEmpty() -> "$addedCount alunos adicionados com sucesso."
            addedCount == 0 -> "Nenhum aluno adicionado. ${ignoredStudents.size} já existiam: ${ignoredStudents.joinToString()}"
            else -> "$addedCount alunos adicionados. ${ignoredStudents.size} ignorados (já existiam): ${ignoredStudents.joinToString()}"
        }

        RepositoryResult(addedCount > 0, message)
    }
}

/**
 * Simple data wrapper for repository operation outcomes.
 * @param isSuccess True if the operation succeeded.
 * @param message A user-readable message describing the result.
 */
data class RepositoryResult(val isSuccess: Boolean, val message: String)