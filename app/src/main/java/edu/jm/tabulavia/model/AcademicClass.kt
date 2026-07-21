/**
 * AcademicClass entity for the 'classes' table.
 * Uses a String-based ID to ensure unique identification across multiple devices
 * and persistent mapping with Firestore.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.util.UUID

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "classes")
data class AcademicClass(

    /**
     * Primary identifier. Initialized with a UUID to prevent collisions during sync.
     */
    @PrimaryKey
    @JsonNames("courseId")
    val classId: String = UUID.randomUUID().toString(),

    /**
     * Name of the class. Default empty string allows Firestore serialization.
     */
    @JsonNames("courseName")
    val className: String = "",

    /**
     * Academic year associated with the class. Default empty string allows Firestore serialization.
     */
    val academicYear: String = "",

    /**
     * Period or term of the class. Default empty string allows Firestore serialization.
     */
    val period: String = "",

    /**
     * Number of class sessions planned.
     */
    val numberOfClasses: Int = 0
)