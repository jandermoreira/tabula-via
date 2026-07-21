/**
 * Entity representing an educational activity linked to a class.
 * Uses a UUID string as the primary key for persistent local and remote identification.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = AcademicClass::class,
            parentColumns = ["classId"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classId"])]
)
data class Activity @JvmOverloads constructor(
    /**
     * Unique identifier generated as a UUID string.
     */
    @PrimaryKey
    val activityId: String = UUID.randomUUID().toString(),

    /**
     * Identifier of the associated class (String UUID).
     */
    @JsonNames("courseId")
    val classId: String = "",

    /**
     * Title of the activity.
     */
    val title: String = "",

    /**
     * Detailed description or type of the activity.
     */
    val description: String = "",

    /**
     * Creation or scheduled timestamp.
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Optional due date timestamp.
     */
    val dueDate: Long? = null
)