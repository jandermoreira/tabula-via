/**
 * Repository for managing Student entities.
 * Handles local Room persistence and Firestore synchronization.
 */
package edu.jm.tabulavia.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import kotlinx.coroutines.tasks.await

class StudentRepository(
    private val studentDao: StudentDao, private val firestore: FirebaseFirestore
) {

    /**
     * Reference to the Firestore collection for a specific user's students.
     */
    private fun userStudentsRef(uid: String) =
        firestore.collection("users").document(uid).collection("students")

    // ----------------- STUDENT -----------------

    /**
     * Inserts or updates a student locally and synchronizes with Firestore.
     * @param student The student entity to persist.
     * @param uid Firebase user identifier.
     * @return The local ID if inserted via Room.
     */
    suspend fun insertStudent(student: Student, uid: String): String {
        // Save locally
        studentDao.insertStudent(student)

        // Prepare Firestore data
        val data = hashMapOf(
            "name" to student.name,
            "displayName" to student.displayName,
            "studentNumber" to student.studentNumber,
            "classId" to student.classId
        )

        return if (student.studentId.isEmpty()) {
            // New Firestore document
            val docRef = userStudentsRef(uid).add(data).await()
            val updatedStudent = student.copy(studentId = docRef.id)
            studentDao.insertStudent(updatedStudent)
            docRef.id
        } else {
            // Update existing document
            userStudentsRef(uid).document(student.studentId).set(data, SetOptions.merge()).await()
            student.studentId
        }

    }

    /**
     * Bulk inserts or updates a list of students locally.
     * @param students List of students.
     */
    suspend fun insertAllStudents(students: List<Student>) {
        studentDao.insertAll(students)
    }

    /**
     * Retrieves all students from a specific class locally.
     * @param classId Identifier of the class.
     * @return List of students in the class.
     */
    suspend fun getStudentsForClass(classId: Long): List<Student> =
        studentDao.getStudentsForClass(classId)

    /**
     * Retrieves a student by their local identifier.
     * @param studentId Identifier of the student.
     * @return Student entity or null if not found.
     */
    suspend fun getStudentById(studentId: String): Student? =
        studentDao.getStudentById(studentId)

    /**
     * Retrieves all students locally.
     * @return List of all students.
     */
    suspend fun getAllStudents(): List<Student> = studentDao.getAllStudents()
}