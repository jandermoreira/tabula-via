package edu.jm.tabulavia.model

import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Data Transfer Object for mapping Firestore documents in the 'evidences' collection.
 */
@IgnoreExtraProperties
data class FirestoreEvidence(
    val evidenceId: String = "",
    val classId: String = "",
    val name: String = "",
    val deadline: Long = 0L,
    val type: String = "MONITORING",
    val scores: Map<String, Double> = emptyMap()
)
