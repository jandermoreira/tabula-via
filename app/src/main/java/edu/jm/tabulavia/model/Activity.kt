package edu.jm.tabulavia.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "activities",
    foreignKeys = [ForeignKey(
        entity = Course::class,
        parentColumns = ["classId"],
        childColumns = ["classId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["classId"])]
)
data class Activity(
    @PrimaryKey(autoGenerate = true)
    val activityId: Long = 0,
    val classId: Long,
    val title: String,
    val description: String,
    val dueDate: Long? = null
)
