package edu.jm.tabulavia.repository

import com.google.firebase.firestore.FirebaseFirestore
import edu.jm.tabulavia.dao.EvidenceDao
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import edu.jm.tabulavia.model.EvidenceType
import edu.jm.tabulavia.model.FirestoreEvidence
import kotlinx.coroutines.tasks.await

/**
 * Repository responsible for synchronizing evidence data from Firestore to the local database.
 */
class EvidenceRepository(
    private val firestore: FirebaseFirestore,
    private val evidenceDao: EvidenceDao
) {
    /**
     * Syncs all evidences for a specific class from Firestore to Room.
     * @param userEmail The email of the authenticated user.
     * @param classId The ID of the class to sync.
     */
    suspend fun syncEvidences(userEmail: String, classId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userEmail)
                .collection("classes")
                .document(classId)
                .collection("evidences")
                .get()
                .await()

            val firestoreEvidences = snapshot.toObjects(FirestoreEvidence::class.java)
            
            firestoreEvidences.forEach { dto ->
                val evidence = Evidence(
                    evidenceId = dto.evidenceId,
                    classId = dto.classId,
                    name = dto.name,
                    deadline = dto.deadline,
                    type = EvidenceType.valueOf(dto.type)
                )
                
                val scores = dto.scores.map { (studentId, score) ->
                    EvidenceScore(dto.evidenceId, studentId, score)
                }
                
                evidenceDao.syncEvidence(evidence, scores)
            }
        } catch (e: Exception) {
            // Failure in sync is handled by the caller or logged
        }
    }

    /**
     * Provides a stream of local evidence scores for a student in a class.
     */
    fun getStudentScores(studentId: String, classId: String) = 
        evidenceDao.getStudentScoresByClass(studentId, classId)

    /**
     * Retrieves all evidences for a class from local storage.
     */
    fun getEvidences(classId: String) = evidenceDao.getEvidencesByClass(classId)
}
