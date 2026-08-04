package edu.jm.tabulavia.repository

import android.util.Log
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.jm.tabulavia.dao.EvidenceDao
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.FirestoreEvidence
import edu.jm.tabulavia.utils.shouldNotifySync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Repository responsible for synchronizing evidence data from Firestore to the local database.
 * Uses real-time listeners to keep local Room database in sync with Firestore.
 */
class EvidenceRepository(
    private val firestore: FirebaseFirestore,
    private val evidenceDao: EvidenceDao
) {
    private var evidencesListener: ListenerRegistration? = null

    /**
     * Starts a Firestore snapshot listener for a specific class's evidences.
     * Synchronizes evidences and their scores in real-time.
     *
     * @param userEmail The email of the authenticated user.
     * @param classId The ID of the class to sync.
     * @param onSyncActivity Optional callback for sync notifications.
     */
    fun startEvidencesSync(userEmail: String, classId: String, onSyncActivity: () -> Unit = {}) {
        stopEvidencesSync()
        var isInitialSnapshot = true

        evidencesListener = firestore.collection("users")
            .document(userEmail)
            .collection("classes")
            .document(classId)
            .collection("evidences")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EvidenceSync", "Firestore listener error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.shouldNotifySync(isInitialSnapshot)) {
                        onSyncActivity()
                    }
                }
                isInitialSnapshot = false

                snapshot?.documentChanges?.forEach { change ->
                    val docId = change.document.id
                    CoroutineScope(Dispatchers.IO).launch {
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val dto = change.document.toObject(FirestoreEvidence::class.java)
                                val evidence = Evidence(
                                    evidenceId = docId,
                                    classId = classId,
                                    name = dto.name,
                                    deadline = dto.deadline,
                                    type = EvidenceType.valueOf(dto.type)
                                )

                                // Map scores without student existence filtering as requested
                                val scores = dto.scores.mapNotNull { (studentId, score) ->
                                    if (score != null) {
                                        EvidenceScore(docId, studentId, score)
                                    } else null
                                }
                                evidenceDao.syncEvidence(evidence, scores)
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Cascade delete is handled by Room if evidence is deleted
                                evidenceDao.deleteEvidenceById(docId)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Stops the active Firestore listener for evidences to prevent leaks.
     */
    fun stopEvidencesSync() {
        evidencesListener?.remove()
        evidencesListener = null
    }

    /**
     * Provides a stream of local evidence scores for a student in a class.
     */
    fun getStudentScores(studentId: String, classId: String) = 
        evidenceDao.getStudentScoresByClass(studentId, classId)

    /**
     * Provides a stream of all local evidence scores for a class.
     */
    fun getAllScoresByClass(classId: String) = 
        evidenceDao.getAllScoresByClass(classId)

    /**
     * Retrieves all evidences for a class from local storage.
     */
    fun getEvidences(classId: String) = evidenceDao.getEvidencesByClass(classId)
}
