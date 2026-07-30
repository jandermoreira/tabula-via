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
import com.google.firebase.firestore.ListenerRegistration
import edu.jm.tabulavia.dao.StudentDao
import edu.jm.tabulavia.model.Student
import edu.jm.tabulavia.model.StudentStatus
import edu.jm.tabulavia.utils.shouldNotifySync
import edu.jm.tabulavia.worker.SyncStudentWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
     * @param email The authenticated user email.
     */
    suspend fun insertStudent(student: Student, email: String) {
        studentDao.insertStudent(student)

        try {
            // Direct write to Firestore for instant propagation
            firestore.collection("users")
                .document(email)
                .collection("classes")
                .document(student.classId)
                .collection("students")
                .document(student.studentId)
                .set(student)
                .await()
        } catch (e: Exception) {
            Log.w("StudentRepository", "Direct sync failed, falling back to Worker: ${e.message}")
            val syncRequest = OneTimeWorkRequestBuilder<SyncStudentWorker>()
                .setInputData(SyncStudentWorker.buildInputData(email, student.classId, student.studentId))
                .build()
            WorkManager.getInstance(applicationContext).enqueue(syncRequest)
        }
    }

    /**
     * Bulk inserts students locally and synchronizes with Firestore using a batch write.
     * Direct paths are used to allow removal of shared helper methods.
     *
     * @param students List of students to insert.
     * @param uid The authenticated user ID.
     */
    suspend fun insertAllStudents(students: List<Student>, email: String) {
        studentDao.insertAll(students)

        val batch = firestore.batch()
        for (student in students) {
            val docRef = firestore.collection("users")
                .document(email)
                .collection("classes")
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
     * @param classId The unique identifier of the class.
     * @return Flow containing the list of students.
     */
    fun getStudentsForClass(classId: String): Flow<List<Student>> =
        studentDao.getStudentsForClass(classId)

    /**
     * Retrieves all students for a specific class as a list.
     *
     * @param classId The unique identifier of the class.
     * @return List of students.
     */
    suspend fun getStudentsForClassList(classId: String): List<Student> =
        withContext(Dispatchers.IO) {
            studentDao.getStudentsForClassList(classId)
        }

    /**
     * Retrieves a student by their unique identifier.
     *
     * @param studentId The unique identifier of the student.
     * @return The student entity or null if not found.
     */
    suspend fun getStudentById(studentId: String): Student? =
        studentDao.getStudentById(studentId)

    /**
     * Observes all students across all classes.
     */
    fun getAllStudentsFlow(): Flow<List<Student>> = studentDao.getAllStudentsFlow()

    /**
     * Retrieves all students as a list.
     */
    fun getAllStudents(): List<Student> = studentDao.getAllStudents()

    /**
     * Performs a soft delete by updating the student status to CANCELLED.
     * This preserves historical data and avoids cascading deletions.
     *
     * @param student The student entity to cancel.
     * @param email The authenticated user email.
     */
    suspend fun deleteStudent(student: Student, email: String) {
        withContext(Dispatchers.IO) {
            val cancelledStudent = student.copy(status = StudentStatus.CANCELLED)
            
            // Update local Room database
            studentDao.insertStudent(cancelledStudent)

            try {
                // Direct remote update for instant propagation
                firestore.collection("users")
                    .document(email)
                    .collection("classes")
                    .document(student.classId)
                    .collection("students")
                    .document(student.studentId)
                    .set(cancelledStudent)
                    .await()
            } catch (e: Exception) {
                Log.w("StudentRepository", "Direct soft delete sync failed, falling back to Worker: ${e.message}")
                // Fallback to the standard sync worker to update the status remotely
                val syncRequest = OneTimeWorkRequestBuilder<SyncStudentWorker>()
                    .setInputData(SyncStudentWorker.buildInputData(email, student.classId, student.studentId))
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(syncRequest)
            }
        }
    }

    /**
     * Checks if a student number is already in use within a specific class.
     */
    suspend fun studentExistsInClass(studentNumber: String, classId: String): Boolean {
        return studentDao.getStudentByNumberInClass(studentNumber, classId) != null
    }

    /**
     * One-shot fetch of all students for a specific class from Firestore.
     */
    suspend fun syncStudentsFromCloud(email: String, classId: String) {
        val snapshot = firestore.collection("users")
            .document(email)
            .collection("classes")
            .document(classId)
            .collection("students")
            .get().await()
        val students = snapshot.toObjects(Student::class.java).filterNotNull()
        if (students.isNotEmpty()) {
            studentDao.insertAll(students)
        }
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
         * Starts a Firestore snapshot listener for a specific class's students.
         * Uses documentChanges to specifically handle ADDED, MODIFIED, and REMOVED events,
         * ensuring the local Room database stays perfectly in sync with Firestore.
         *
         * @param email The authenticated user email.
         * @param classId The unique identifier of the class.
         * @param onSyncActivity Callback triggered when a remote change is detected.
         */
        fun startStudentsSync(email: String, classId: String, onSyncActivity: () -> Unit = {}) {
            stopStudentsSync()
            var isInitialSnapshot = true

            studentsListener = firestore.collection("users")
                .document(email)
                .collection("classes")
                .document(classId)
                .collection("students")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("StudentRepository", "Firestore listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // Pulse only for local writes (pending) or remote changes (ignoring initial load)
                        if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                            onSyncActivity()
                        }
                    }
                    isInitialSnapshot = false

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
                                    // If a student is physically removed from Firestore, 
                                    // we mark them as CANCELLED locally to maintain integrity.
                                    val existingStudent = studentDao.getStudentById(docId)
                                    existingStudent?.let {
                                        studentDao.insertStudent(it.copy(status = StudentStatus.CANCELLED))
                                    }
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