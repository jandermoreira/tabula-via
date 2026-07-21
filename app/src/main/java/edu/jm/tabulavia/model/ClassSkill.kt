/**
 * ClassSkill entity for the 'class_skills' table.
 * Represents the skills associated with a specific class.
 */

package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "class_skills",
    primaryKeys = ["classId", "skillName"],
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
data class ClassSkill(

    /**
     * Identifier of the associated class.
     */
    @JsonNames("courseId")
    val classId: String = "",

    /**
     * Name of the skill associated with the class.
     */
    val skillName: String = "",

    /**
     * Firestore document identifier.
     */
    val firestoreId: String? = null
)