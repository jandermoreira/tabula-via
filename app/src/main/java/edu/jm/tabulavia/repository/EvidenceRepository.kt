package edu.jm.tabulavia.repository

import android.util.Log
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
            Log.d("EvidenceSync", "Sincronizando ${firestoreEvidences.size} evidências para a turma $classId")
            
            // Fetch existing student IDs to ensure referential integrity
            val existingStudentIds = evidenceDao.getExistingStudentIds(classId).toSet()

            firestoreEvidences.forEach { dto ->
                val evidence = Evidence(
                    evidenceId = dto.evidenceId,
                    classId = dto.classId,
                    name = dto.name,
                    deadline = dto.deadline,
                    type = EvidenceType.valueOf(dto.type)
                )
                
                val scores = dto.scores.mapNotNull { (studentId, score) ->
                    if (score != null && studentId in existingStudentIds) {
                        EvidenceScore(dto.evidenceId, studentId, score)
                    } else {
                        if (score == null && studentId in existingStudentIds) {
                            Log.w("EvidenceSync", "Nota nula ignorada para aluno $studentId na evidência ${dto.evidenceId}")
                        } else if (studentId !in existingStudentIds) {
                            Log.w("EvidenceSync", "Ignorando nota para aluno $studentId (não encontrado na turma $classId)")
                        }
                        null
                    }
                }
                
                evidenceDao.syncEvidence(evidence, scores)
            }
        } catch (e: Exception) {
            Log.e("EvidenceSync", "Erro ao sincronizar evidências: ${e.message}", e)
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
