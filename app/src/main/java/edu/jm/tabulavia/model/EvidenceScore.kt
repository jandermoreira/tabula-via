package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Stores individual student scores for a specific evidence.
 * Maps the 'scores' Map from Firestore into a relational structure.
 */
@Entity(
    tableName = "evidence_scores",
    primaryKeys = ["evidenceId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = Evidence::class,
            parentColumns = ["evidenceId"],
            childColumns = ["evidenceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class EvidenceScore(
    val evidenceId: String,
    val studentId: String,
    val score: Double
)
