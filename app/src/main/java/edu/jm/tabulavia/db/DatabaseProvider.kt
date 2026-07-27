/**
 * File: DatabaseProvider.kt
 * Description: Singleton provider for the AppDatabase instance, configured to handle 
 * flavor-specific database names and migration strategies.
 */

package edu.jm.tabulavia.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import edu.jm.tabulavia.BuildConfig

/**
 * Manages the singleton instance of the Room database.
 */
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    /**
     * Migration from version 13 to 14: Adds 'status' column to 'students' table.
     */
    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE students ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        }
    }

    /**
     * Migration from version 14 to 15: Renames 'numberOfClasses' to 'numberOfSessions' in 'classes' table.
     */
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE classes RENAME COLUMN numberOfClasses TO numberOfSession")
        }
    }

    /**
     * Migration from version 14 to 15: Renames 'numberOfSession' to 'numberOfSessions' in 'classes' table.
     */
    private val MIGRATION_15_16 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE classes RENAME COLUMN numberOfSession TO numberOfSessions")
        }
    }

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
            ).addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)

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