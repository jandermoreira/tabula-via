package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "activity_highlighted_skills",
    primaryKeys = ["activityId", "skillName"],
    foreignKeys = [
        ForeignKey(
            entity = edu.jm.tabulavia.model.Activity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ActivityHighlightedSkill(
    val activityId: Long,
    val skillName: String
)