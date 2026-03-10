/**
 * Repository for cloud synchronization via Firebase Storage.
 * Handles backup and restoration of application data using Firebase Storage.
 */
package edu.jm.tabulavia.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import edu.jm.tabulavia.model.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Repository for uploading and downloading backup data to Firebase Storage.
 */
class CloudStorageRepository(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * Uploads backup data to Firebase Storage.
     * @param backupData Data object containing all entities to backup.
     * @return RepositoryResult indicating success or failure.
     */
    suspend fun uploadBackupData(backupData: BackupData): RepositoryResult = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext RepositoryResult(false, "Usuário não logado.")

        try {
            val jsonString = json.encodeToString(backupData)
            val storageRef = storage.reference.child("backups/$userId/backup.json")
            storageRef.putBytes(jsonString.toByteArray()).await()
            RepositoryResult(true, "Backup concluído com sucesso!")
        } catch (e: Exception) {
            RepositoryResult(false, "Erro no backup: ${e.message}")
        }
    }

    /**
     * Downloads backup data from Firebase Storage.
     * @return RestoreResult containing BackupData or null and a message.
     */
    suspend fun downloadBackupData(): RestoreResult = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext RestoreResult(null, "Usuário não logado.")

        try {
            val storageRef = storage.reference.child("backups/$userId/backup.json")
            val bytes = storageRef.getBytes(10 * 1024 * 1024).await()
            val jsonString = String(bytes)
            val backupData = json.decodeFromString<BackupData>(jsonString)
            RestoreResult(backupData, "Restauração concluída com sucesso!")
        } catch (e: Exception) {
            RestoreResult(null, "Erro na restauração: ${e.message}")
        }
    }
}

/**
 * Result wrapper for upload operations.
 */
data class RepositoryResult(val success: Boolean, val message: String)

/**
 * Result wrapper for restoration operations.
 */
data class RestoreResult(val data: BackupData?, val message: String)