package edu.jm.classsupervision.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class Class(
    @PrimaryKey(autoGenerate = true)
    val classId: Long = 0,
    val className: String,
    val academicYear: String,
    val period: String
)
