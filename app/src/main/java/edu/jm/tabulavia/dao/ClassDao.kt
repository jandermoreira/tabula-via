/**
 * Data Access Object for the AcademicClass entity.
 * Provides methods to interact with the 'classes' table in the database.
 */
package edu.jm.tabulavia.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.jm.tabulavia.model.AcademicClass
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {

    /**
     * Inserts a single class into the database.
     * Replaces the existing entry if there is a conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(clazz: AcademicClass)

    /**
     * Inserts a list of classes into the database.
     * Replaces existing entries in case of conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(classes: List<AcademicClass>)

    /**
     * Retrieves a specific class based on its unique identifier.
     * Returns null if no class is found with the given ID.
     */
    @Query("SELECT * FROM classes WHERE classId = :classId")
    suspend fun getClassById(classId: String): AcademicClass?

    /**
     * Updates a specific class based on its unique identifier.
     */
    @Update
    suspend fun updateClass(clazz: AcademicClass)

    /**
     * Retrieves all classes from the database.
     * Results are ordered alphabetically by class name.
     */
    @Query("SELECT * FROM classes ORDER BY className ASC")
    fun getAllClassesFlow(): Flow<List<AcademicClass>>

    /**
     * Retrieves all classes from the database as a one-time list.
     * Results are ordered alphabetically by class name.
     */
    @Query("SELECT * FROM classes ORDER BY className ASC")
    suspend fun getAllClasses(): List<AcademicClass>

    /**
     * Deletes a class record from the database.
     *
     * @param clazz The class entity to be deleted.
     */
    @Delete
    suspend fun deleteClass(clazz: AcademicClass)
}