/**
 * File: DatabaseProvider.kt
 * Description: Singleton provider for the AppDatabase instance, configured to handle 
 * flavor-specific database names and migration strategies.
 */

package edu.jm.tabulavia.db

import android.content.Context
import androidx.room.Room
import edu.jm.tabulavia.BuildConfig

/**
 * Manages the singleton instance of the Room database.
 */
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    /**
     * Provides access to the AppDatabase instance.
     * * @param context The application context to prevent memory leaks.
     * @return The synchronized singleton instance of AppDatabase.
     */
    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            /* * Initialize the Room database using the flavor-specific name 
             * defined in the build.gradle configuration.
             */
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                BuildConfig.DATABASE_NAME
            )

            /*
             * Apply destructive migration only for the development environment
             * to facilitate rapid schema changes
             */
            if (BuildConfig.FLAVOR == "dev") {
                builder.fallbackToDestructiveMigration()
            }

            val newInstance = builder.build()
            instance = newInstance
            newInstance
        }
    }
}