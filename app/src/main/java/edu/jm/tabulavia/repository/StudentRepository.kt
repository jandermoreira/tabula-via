/**
 * Repository for managing Student entities.
 * Handles local Room persistence and Firestore synchronization using persistent String IDs.
 */
package edu.jm.tabulavia.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class StudentRepository(
    private val studentDao: StudentDao,
    private val firestore: FirebaseFirestore
) {

    /**
     * Reference to the Firestore document for a specific student.
     * Path: users/{uid}/courses/{classId}/students/{studentId}
     */
    private fun studentDocRef(uid: String, classId: String, studentId: String) =
        firestore.collection("users")
            .document(uid)
            .collection("courses")
            .document(classId)
            .collection("students")
            .document(studentId)

    // ----------------- STUDENT -----------------

    /**
     * Inserts or updates a student locally and synchronizes with Firestore.
     * Uses the studentId (UUID) provided by the model.
     */
    suspend fun insertStudent(student: Student, uid: String) {
        // Persistência Local (Room)
        studentDao.insertStudent(student)

        // Sincronização Remota (Firestore)
        studentDocRef(uid, student.classId, student.studentId)
            .set(student)
            .await()
    }

    /**
     * Bulk inserts or updates a list of students locally.
     * Useful for restoration or bulk migrations.
     */
    suspend fun insertAllStudents(students: List<Student>) {
        studentDao.insertAll(students)
    }

    /**
     * Observes all students from a specific class locally using String ID.
     * Returns a Flow that emits updates whenever the local database changes.
     */
    fun getStudentsForClass(classId: String): Flow<List<Student>> =
        studentDao.getStudentsForClass(classId)

    /**
     * Retrieves a student by their unique String identifier.
     */
    suspend fun getStudentById(studentId: String): Student? =
        studentDao.getStudentById(studentId)

    /**
     * Observes all students across all courses locally.
     */
    fun getAllStudents(): Flow<List<Student>> = studentDao.getAllStudents()
}