/**
 * Entity representing the membership of a student in an activity group.
 * Uses a composite primary key with String identifiers for persistent mapping.
 */
package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "group_members",
    primaryKeys = ["activityId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = Activity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["studentId"])
    ]
)
data class GroupMember(
    /**
     * Identifier of the associated activity (String UUID).
     */
    val activityId: String,

    /**
     * Identifier of the associated student (String UUID).
     */
    val studentId: String,

    /**
     * Group number assigned to the student within the activity.
     */
    val groupNumber: Int
)