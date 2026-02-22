/**
 * Repository for cloud synchronization via Firebase Storage.
 * Matches CourseViewModel logic for backup and restoration.
 */

package edu.jm.tabulavia.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import edu.jm.tabulavia.model.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CloudStorageRepository(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    // Matches the default Json configuration used in the original project
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Uploads the backup data to Firebase Storage.
     * Logic identical to the 'backup' function in CourseViewModel.
     */
    suspend fun uploadBackupData(backupData: BackupData): RepositoryResult = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext RepositoryResult(false, "Usuário não logado.")

        try {
            val jsonString = json.encodeToString(BackupData.serializer(), backupData)
            val storageRef = storage.reference.child("backups/$userId/backup.json")

            storageRef.putBytes(jsonString.toByteArray()).await()
            RepositoryResult(true, "Backup concluído com sucesso!")
        } catch (e: Exception) {
            RepositoryResult(false, "Erro no backup: ${e.message}")
        }
    }

    /**
     * Downloads the backup data from Firebase Storage.
     * Limits and behavior match the original ViewModel restoration logic.
     */
    suspend fun downloadBackupData(): RestoreResult = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext RestoreResult(null, "Usuário não logado.")

        try {
            val storageRef = storage.reference.child("backups/$userId/backup.json")

            val bytes = storageRef.getBytes(10 * 1024 * 1024).await()
            val jsonString = String(bytes)
            val data = json.decodeFromString(BackupData.serializer(), jsonString)

            RestoreResult(data, "Restauração concluída com sucesso!")
        } catch (e: Exception) {
            RestoreResult(null, "Erro na restauração: ${e.message}")
        }
    }
}

/**
 * Result wrapper for restoration operations.
 */
data class RestoreResult(val data: BackupData?, val message: String)