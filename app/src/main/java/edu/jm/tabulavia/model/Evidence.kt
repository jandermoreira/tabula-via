package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local representation of an evidence source metadata.
 */
@Entity(tableName = "evidences")
data class Evidence(
    @PrimaryKey
    val evidenceId: String,
    val classId: String,
    val name: String,
    val deadline: Long,
    val type: EvidenceType
)
