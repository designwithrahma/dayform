package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryEntity
import com.example.data.model.SubtaskEntity
import com.example.data.model.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class DayformDatabase : RoomDatabase() {
    abstract fun dayformDao(): DayformDao

    companion object {
        const val DATABASE_NAME = "dayform_tasks.db"

        /**
         * Explicit non-destructive migration from version 1 to version 2.
         * Adds nullable columns for recurring series identity (`recurrenceSeriesId`),
         * idempotent occurrence tracking (`spawnedNextTaskId`), and monthly recurrence
         * anchor day (`recurrenceAnchorDay`). Preserves 100% of existing user data.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceSeriesId` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `spawnedNextTaskId` INTEGER")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `recurrenceAnchorDay` INTEGER")
            }
        }

        /**
         * Explicit forward-only non-destructive schema migration registry.
         * All version increments must register a non-destructive [Migration] here
         * and preserve exported schemas under `app/schemas/`.
         * Destructive migration fallbacks are strictly prohibited in production.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var INSTANCE: DayformDatabase? = null

        fun getInstance(context: Context): DayformDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayformDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun setTestInstance(database: DayformDatabase) {
            INSTANCE = database
        }

        fun clearTestInstance() {
            INSTANCE = null
        }
    }
}
