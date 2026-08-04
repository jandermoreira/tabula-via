package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import edu.jm.tabulavia.model.Evidence
import edu.jm.tabulavia.model.EvidenceScore
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Evidence and EvidenceScore entities.
 */
@Dao
interface EvidenceDao {

    /**
     * Inserts or updates an evidence record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvidence(evidence: Evidence)

    /**
     * Inserts or updates a list of evidence scores.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<EvidenceScore>)

    /**
     * Atomic transaction to sync an evidence and its associated scores.
     */
    @Transaction
    suspend fun syncEvidence(evidence: Evidence, scores: List<EvidenceScore>) {
        insertEvidence(evidence)
        insertScores(scores)
    }

    /**
     * Retrieves all evidences for a specific class ordered by deadline.
     */
    @Query("SELECT * FROM evidences WHERE classId = :classId ORDER BY deadline ASC")
    fun getEvidencesByClass(classId: String): Flow<List<Evidence>>

    /**
     * Retrieves all scores for a specific evidence.
     */
    @Query("SELECT * FROM evidence_scores WHERE evidenceId = :evidenceId")
    suspend fun getScoresByEvidence(evidenceId: String): List<EvidenceScore>

    /**
     * Retrieves all scores for a specific student in a class, ordered by evidence deadline.
     */
    @Query("""
        SELECT es.* FROM evidence_scores es
        JOIN evidences e ON es.evidenceId = e.evidenceId
        WHERE es.studentId = :studentId AND e.classId = :classId
        ORDER BY e.deadline ASC
    """)
    fun getStudentScoresByClass(studentId: String, classId: String): Flow<List<EvidenceScore>>

    /**
     * Deletes all evidences and cascading scores for a class.
     */
    @Query("DELETE FROM evidences WHERE classId = :classId")
    suspend fun deleteEvidencesByClass(classId: String)

    /**
     * Retrieves all existing student IDs for a specific class to ensure referential integrity.
     */
    @Query("SELECT studentId FROM students WHERE classId = :classId")
    suspend fun getExistingStudentIds(classId: String): List<String>
}
