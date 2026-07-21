/**
 * ClassSession entity for the 'class_sessions' table.
 * Represents a specific class meeting, isolated by class and persistent via String ID.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = AcademicClass::class,
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
     * Reference to the associated class ID.
     */
    @JsonNames("courseId")
    val classId: String,

    /**
     * Timestamp representing when the session occurred.
     */
    val timestamp: Long = System.currentTimeMillis()
)