/**
 * ClassSession entity for the 'class_sessions' table.
 * Represents a specific class meeting, isolated by course and persistent via String ID.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["classId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class ClassSession(

    /**
     * Unique identifier for the session.
     * Must be a UUID generated at creation or synchronized from Firestore.
     */
    @PrimaryKey
    val sessionId: String,

    /**
     * Legacy or external Firestore document identifier if mapped differently.
     */
    val firestoreId: String? = null,

    /**
     * Reference to the associated course ID.
     */
    val classId: String,

    /**
     * Timestamp representing when the session occurred.
     */
    val timestamp: Long = System.currentTimeMillis()
)