package edu.jm.classsupervision.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.classsupervision.model.Class

@Dao
interface ClassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(classEntity: Class)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classes: List<Class>) // Para restauração

    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllClasses(): List<Class>

    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getClassById(classId: Long): Class?
}
