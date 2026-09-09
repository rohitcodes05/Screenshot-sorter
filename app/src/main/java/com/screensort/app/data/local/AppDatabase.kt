package com.screensort.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScreenshotEntity::class, UserCategoryEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun screenshotDao(): ScreenshotDao
    abstract fun userCategoryDao(): UserCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2:
         * Adds the `visualLabels` column for ML Kit Image Labeling concepts.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE screenshots ADD COLUMN visualLabels TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Migration from version 2 to 3:
         * Creates the `user_categories` table for custom user-defined categories.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        dateCreated INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 3 to 4:
         * Adds performance indexes on (category, dateAdded) and (dateAdded) in screenshots table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_screenshots_category_dateAdded` ON `screenshots` (`category`, `dateAdded`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_screenshots_dateAdded` ON `screenshots` (`dateAdded`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "screensort_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
