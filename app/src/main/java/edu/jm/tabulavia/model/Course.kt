/**
 * Course entity for the 'classes' table.
 * Uses a String-based ID to ensure unique identification across multiple devices
 * and persistent mapping with Firestore.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "classes")
data class Course(

    /**
     * Primary identifier. Should be initialized with a UUID or
     * the Firestore document ID to prevent collisions during sync.
     */
    @PrimaryKey
    val classId: String = UUID.randomUUID().toString(),

    /**
     * Name of the class.
     */
    val className: String,

    /**
     * Academic year associated with the class.
     */
    val academicYear: String,

    /**
     * Period or term of the class.
     */
    val period: String,

    /**
     * Number of class sessions planned.
     */
    val numberOfClasses: Int = 0
)