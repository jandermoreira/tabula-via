/**
 * ActivityHighlightedSkill entity for the 'activity_highlighted_skills' table.
 * Represents the skills emphasized in a specific activity.
 */

package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "activity_highlighted_skills",
    primaryKeys = ["activityId", "skillName"],
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class ActivityHighlightedSkill(

    /**
     * Identifier of the associated activity.
     */
    val activityId: Long,

    /**
     * Name of the highlighted skill.
     */
    val skillName: String,

    /**
     * Firestore document identifier.
     */
    val firestoreId: String? = null
)