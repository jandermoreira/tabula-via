/**
 * Repository for managing Student entities.
 * Handles local Room persistence and Firestore synchronization using persistent String IDs.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.worker.SyncStudentWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentRepository(
    private val studentDao: StudentDao,
    private val firestore: FirebaseFirestore,
    private val attendanceRepository: AttendanceRepository,
    private val applicationContext: Context
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
     * Inserts or updates a student locally and enqueues a synchronization job with Firestore.
     * Removed await() to support offline-first behavior.
     *
     * @param student The student to insert or update.
     * @param uid The authenticated user ID.
     */
    suspend fun insertStudent(student: Student, uid: String) {
        studentDao.insertStudent(student)

        // Enqueue a WorkManager job to synchronize the student with Firestore
        val syncRequest = OneTimeWorkRequestBuilder<SyncStudentWorker>()
            .setInputData(SyncStudentWorker.buildInputData(uid, student.classId, student.studentId))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(syncRequest)
    }

    /**
     * Bulk inserts students locally and synchronizes with Firestore using a batch write.
     * Uses a single Room transaction and a Firestore batch without awaiting network.
     */
    suspend fun insertAllStudents(students: List<Student>, uid: String) {
        studentDao.insertAll(students)

        val batch = firestore.batch()
        for (student in students) {
            val docRef = studentDocRef(uid, student.classId, student.studentId)
            batch.set(docRef, student)
        }
        batch.commit()
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
    fun getAllStudentsFlow(): Flow<List<Student>> = studentDao.getAllStudentsFlow()

    /**
     * Retrieves all students as a list.
     */
    fun getAllStudents(): List<Student> = studentDao.getAllStudents()

    /**
     * Deletes a student from both local and remote storage, ensuring that all
     * associated attendance records are removed beforehand to avoid foreign key
     * violations.
     *
     * @param student The student entity to delete.
     * @param uid The authenticated user ID.
     */
    suspend fun deleteStudent(student: Student, uid: String) {
        withContext(Dispatchers.IO) {
            attendanceRepository.removeStudentFromAttendanceSessions(
                studentId = student.studentId,
                classId = student.classId,
                uid = uid
            )
            studentDocRef(uid, student.classId, student.studentId).delete()
            studentDao.deleteStudent(student)
        }
    }

    /**
     * Checks whether a student with a specific student number exists within a given class.
     */
    suspend fun studentExistsInClass(studentNumber: String, classId: String): Boolean {
        return studentDao.getStudentByNumberInClass(studentNumber, classId) != null
    }

    /**
     * Retrieves the list of student numbers associated with a specific class.
     */
    suspend fun getExistingStudentNumbersForClass(classId: String): List<String> {
        return studentDao.getStudentNumbersForClass(classId)
    }
}