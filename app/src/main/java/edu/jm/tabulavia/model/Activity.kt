/**
 * Entity representing an educational activity linked to a course.
 * Uses a UUID string as the primary key for persistent local and remote identification.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class Activity(
    /**
     * Unique identifier generated as a UUID string.
     */
    @PrimaryKey
    val activityId: String = UUID.randomUUID().toString(),

    /**
     * Identifier of the associated course (String UUID).
     */
    val classId: String,

    /**
     * Title of the activity.
     */
    val title: String,

    /**
     * Detailed description or type of the activity.
     */
    val description: String,

    /**
     * Creation or scheduled timestamp.
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Optional due date timestamp.
     */
    val dueDate: Long? = null
)