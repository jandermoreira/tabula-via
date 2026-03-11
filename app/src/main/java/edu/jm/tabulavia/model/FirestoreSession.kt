/**
 * Data Transfer Object (DTO) for Firestore synchronization.
 * This class ensures that session data is serialized correctly to the cloud
 * using the project's String-based identifier pattern.
 */
package edu.jm.tabulavia.model

import kotlinx.serialization.Serializable

@Serializable
data class FirestoreSession(

    /**
     * Unique identifier for the session, matching the ClassSession primary key.
     */
    val sessionId: String = "",

    /**
     * Reference to the course identifier.
     */
    val classId: String = "",

    /**
     * Date and time of the session in milliseconds.
     */
    val timestamp: Long = 0L,

    /**
     * Map of student identifiers to their respective attendance status names.
     */
    val attendance: Map<String, String> = emptyMap()
)