package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(entity = Activity::class, parentColumns = ["activityId"], childColumns = ["activityId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["studentId"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class GroupMember(
    @PrimaryKey(autoGenerate = true)
    val groupMemberId: Long = 0,
    val activityId: Long,
    val studentId: Long,
    val groupNumber: Int
)
