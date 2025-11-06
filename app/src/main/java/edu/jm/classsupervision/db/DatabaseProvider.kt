package edu.jm.classsupervision.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        // Retorna a instância se já existir, senão, cria uma nova.
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "class_supervision_database" // Nome do arquivo do banco de dados
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
