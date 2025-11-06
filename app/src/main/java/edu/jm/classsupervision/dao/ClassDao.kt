package edu.jm.classsupervision.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import edu.jm.classsupervision.model.Class

@Dao
interface ClassDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClass(aClass: Class)

    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllClasses(): List<Class>

    // Função que busca uma única turma pelo seu ID.
    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getClassById(classId: Long): Class?
}
