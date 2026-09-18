package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entities.*

@Database(
    entities = [
        PrayerRecordEntity::class,
        HabitRecordEntity::class,
        CounterRecordEntity::class,
        DailyReflectionEntity::class,
        WeeklyReportEntity::class,
        UserSettingsEntity::class,
        DayRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao
    abstract fun habitDao(): HabitDao
    abstract fun counterDao(): CounterDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun weeklyReportDao(): WeeklyReportDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun dayRecordDao(): DayRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_records` (
                        `dayKey` TEXT NOT NULL,
                        `isFinalized` INTEGER NOT NULL DEFAULT 0,
                        `finalizedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`dayKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ahl_alquran.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
