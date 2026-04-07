/**
 * Repository for managing Student entities.
 * Handles local Room persistence and Firestore synchronization using Worker-based operations
 * to ensure data integrity and offline-first support.
 */
package edu.jm.tabulavia.repository

import android.content.Context
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.worker.SyncDeleteStudentWorker
import edu.jm.tabulavia.worker.SyncStudentWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class StudentRepository(
    private val studentDao: StudentDao,
    private val firestore: FirebaseFirestore,
    private val attendanceRepository: AttendanceRepository,
    private val applicationContext: Context
) {

    // ----------------- STUDENT -----------------

    /**
     * Inserts or updates a student locally and enqueues a synchronization job with Firestore.
     *
     * @param student The student to insert or update.
     * @param uid The authenticated user ID.
     */
    suspend fun insertStudent(student: Student, uid: String) {
        studentDao.insertStudent(student)

        val syncRequest = OneTimeWorkRequestBuilder<SyncStudentWorker>()
            .setInputData(SyncStudentWorker.buildInputData(uid, student.classId, student.studentId))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(syncRequest)
    }

    /**
     * Bulk inserts students locally and synchronizes with Firestore using a batch write.
     * Direct paths are used to allow removal of shared helper methods.
     *
     * @param students List of students to insert.
     * @param uid The authenticated user ID.
     */
    suspend fun insertAllStudents(students: List<Student>, uid: String) {
        studentDao.insertAll(students)

        val batch = firestore.batch()
        for (student in students) {
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("courses")
                .document(student.classId)
                .collection("students")
                .document(student.studentId)
            batch.set(docRef, student)
        }
        batch.commit()
    }

    /**
     * Observes all students from a specific class.
     *
     * @param classId The unique identifier of the course.
     * @return Flow containing the list of students.
     */
    fun getStudentsForClass(classId: String): Flow<List<Student>> =
        studentDao.getStudentsForClass(classId)

    /**
     * Retrieves a student by their unique identifier.
     *
     * @param studentId The unique identifier of the student.
     * @return The student entity or null if not found.
     */
    suspend fun getStudentById(studentId: String): Student? =
        studentDao.getStudentById(studentId)

    /**
     * Observes all students across all courses.
     */
    fun getAllStudentsFlow(): Flow<List<Student>> = studentDao.getAllStudentsFlow()

    /**
     * Retrieves all students as a list.
     */
    fun getAllStudents(): List<Student> = studentDao.getAllStudents()

    /**
     * Deletes a student from local and remote storage.
     * Prioritizes local deletion and enqueues background workers for remote cleanup
     * to ensure the UI updates immediately even if offline.
     *
     * @param student The student entity to delete.
     * @param uid The authenticated user ID.
     */
    suspend fun deleteStudent(student: Student, uid: String) {
        withContext(Dispatchers.IO) {
            try {
                // Remove local attendance records first to maintain integrity
                attendanceRepository.removeStudentFromAttendanceSessions(
                    studentId = student.studentId,
                    classId = student.classId,
                    uid = uid
                )
            } catch (e: Exception) {
                // Logs failure but allows student deletion to proceed
                Log.e("StudentRepository", "Failed to clear attendance: ${e.message}")
            }

            // Perform local deletion from Room
            studentDao.deleteStudent(student)

            // Enqueue worker for remote Firestore student document deletion
            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncDeleteStudentWorker>()
                .setInputData(
                    SyncDeleteStudentWorker.buildInputData(
                        userId = uid,
                        classId = student.classId,
                        studentId = student.studentId
                    )
                )
                .build()

            WorkManager.getInstance(applicationContext).enqueue(syncWorkRequest)
        }
    }

    /**
     * Checks if a student number is already in use within a specific class.
     */
    suspend fun studentExistsInClass(studentNumber: String, classId: String): Boolean {
        return studentDao.getStudentByNumberInClass(studentNumber, classId) != null
    }

    /**
     * Retrieves all registered student numbers for a class.
     */
    suspend fun getExistingStudentNumbersForClass(classId: String): List<String> {
        return studentDao.getStudentNumbersForClass(classId)
    }

    // -- Listener
    private var studentsListener: ListenerRegistration? = null

    /**
         * Starts a Firestore snapshot listener for a specific course's students.
         * Uses documentChanges to specifically handle ADDED, MODIFIED, and REMOVED events,
         * ensuring the local Room database stays perfectly in sync with Firestore.
         *
         * @param uid The authenticated user ID.
         * @param classId The unique identifier of the course.
         */
        fun startStudentsSync(uid: String, classId: String) {
            stopStudentsSync()

            studentsListener = firestore.collection("users")
                .document(uid)
                .collection("courses")
                .document(classId)
                .collection("students")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("StudentRepository", "Firestore listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        // The document ID is the primary key (studentId)
                        val docId = change.document.id

                        CoroutineScope(Dispatchers.IO).launch {
                            when (change.type) {
                                DocumentChange.Type.ADDED,
                                DocumentChange.Type.MODIFIED -> {
                                    val student = change.document.toObject(Student::class.java)
                                    val studentToSync = student.copy(studentId = docId)
                                    studentDao.insertStudent(studentToSync)
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val studentToDelete = Student(studentId = docId)
                                    studentDao.deleteStudent(studentToDelete)
                                }
                            }
                        }
                    }
                }
        }

        /**
         * Stops the active Firestore listener to prevent memory leaks.
         */
        fun stopStudentsSync() {
            studentsListener?.remove()
            studentsListener = null
        }
}